package com.omnix.manager.search;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.omnix.config.Config;
import com.omnix.config.UiValidationException;
import com.omnix.manager.IndexManager;
import com.omnix.manager.IndexSearchInfo;
import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.ColumnInfoManager;
import com.omnix.manager.parser.DocumentMapper;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.parser.ScriptInfo;
import com.omnix.manager.parser.ScriptInfoManager;
import com.omnix.manager.recieve.LogBean;
import com.omnix.manager.websocket.SearchStatus;
import com.omnix.manager.websocket.WebSocketManager;
import com.omnix.util.DateUtils;
import com.omnix.util.ParseUtils;
import com.omnix.util.QueryBuilder;
import com.omnix.util.SearchUtils;

@Service
public class IndexSearchManager {
	@Autowired
	private Config config;
	@Autowired
	private IndexManager indexManager;
	@Autowired
	private WebSocketManager webSocketManager;

	/** JobId Cache **/
	private LoadingCache<Long, SearchResult> jobCache;

	private static final DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter defaultFormatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	/** Logger */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@PostConstruct
	public void init() {
		logger.info("search cache init. max hour : {}", config.getSearchCacheMaxHour());
		jobCache = CacheBuilder.newBuilder().refreshAfterWrite(config.getSearchCacheMaxHour(), TimeUnit.HOURS).removalListener(new RemovalListener<Long, SearchResult>() {
			@Override
			public void onRemoval(RemovalNotification<Long, SearchResult> entry) {
				entry.getValue().setFinish(true);
			}

		}).build(CacheLoader.<Long, SearchResult>from(jobId -> {
			SearchResult searchResult = new SearchResult(jobId);
			return searchResult;
		}));
	}

	/**
	 * 해당 검색 범위에 count만 빠르게 가져온다.
	 */
	public SearchResult searchCount(SearchSupport searchSupport, long tableId, boolean delete) {
		final SearchResult searchResult = makeSearchResult(searchSupport.getJobId());

		logger.info("search start : {}", searchSupport.getJobId());

		/** table 체크 */
		Map<String, ColumnInfo> columnInfoCache = ColumnInfoManager.COLUMNINFO_CACHE.get(tableId);
		if (null == columnInfoCache) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		/** 검색 범위 계산 */
		List<LocalDateTime> lists = SearchUtils.getBetweenDate(searchSupport.getSearchType(), searchSupport.getDateRange());
		LocalDateTime start = lists.get(0);
		LocalDateTime finish = lists.get(1);

		QueryBuilder builder = new QueryBuilder(searchSupport.getQuery(), columnInfoCache);
		Query subQuery;
		try {
			subQuery = builder.build();
		} catch (Exception e) {
			logger.error("query build exception", e);
			String[] args = { e.getMessage() };
			throw new UiValidationException("ALERT_SEARCH_CHECK_FAIL", args);
		}

		/** 쿼리를 만든다. */
		Builder rangeQueryBuilder = new BooleanQuery.Builder();
		if (searchSupport.getSearchType() == SearchType.CUSTOM || searchSupport.getSearchType() == SearchType.HOUR) { // 사용자 정의 영역에서만 range filter 처리를 한다.
			rangeQueryBuilder.add(LongPoint.newRangeQuery("_date", ParseUtils.makeTimeStamp(start), ParseUtils.makeTimeStamp(finish)), Occur.MUST);
		}

		rangeQueryBuilder.add(subQuery, Occur.MUST);
		Query rangeQuery = rangeQueryBuilder.build();

		Builder queryBuilder = new BooleanQuery.Builder();
		queryBuilder.add(subQuery, Occur.MUST);
		Query query = queryBuilder.build();

		if (delete) {
			logger.info("Delete Query : {}", rangeQuery);
		} else {
			logger.info("Query : {}", rangeQuery);
		}

		/** 검색 대상을 가져온다. */
		List<String> targets = SearchUtils.searchTargetList(start, finish, false);

		/** range 대상을 찾는다. */
		String rangeKey1 = targets.get(0);
		String rangeKey2 = targets.get(targets.size() - 1);

		Map<String, Integer> result = new ConcurrentHashMap<>();
		AtomicLong totalCount = new AtomicLong(0L);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		AtomicInteger processCount = new AtomicInteger(0);
		targets.parallelStream().forEach(dateKey -> {
			if (searchResult.isFinish()) {
				logger.debug("search process cancel : {}", searchResult.getJobId());
				return;
			}

			Optional<IndexSearchInfo> optional = indexManager.acquireSearcher(tableId, dateKey);
			optional.ifPresent(indexSearchInfo -> {
				try {
					int count = 0;
					if (StringUtils.equals(rangeKey1, dateKey) || StringUtils.equals(rangeKey2, dateKey)) { // 해당 case는 rangeQuery 대상임.
						count = indexSearchInfo.getIndexSearcher().count(rangeQuery);

						if (delete) {
							indexManager.deleteIndex(tableId, dateKey, rangeQuery);
						}

					} else {
						count = indexSearchInfo.getIndexSearcher().count(query);

						if (delete) {
							indexManager.deleteIndex(tableId, dateKey, rangeQuery);
						}
					}

					result.put(dateKey, count);
					long total = totalCount.addAndGet(count);

					if (searchSupport.getLimit() > 0 && total >= searchSupport.getLimit()) {
						searchResult.setFinish(true);
					}

				} catch (IOException e) {
					logger.error("search error", e);

				} finally {
					indexManager.releaseSearcher(indexSearchInfo);
				}

			});
			
			/** 진행율 전송 */
			int currentCount = processCount.incrementAndGet();
			double percent = ((double) currentCount / targets.size()) * 100;
			webSocketManager.sendSearchStatus(new SearchStatus(searchSupport.getJobId(), totalCount.get(), percent, false, "count"));

		});

		stopWatch.stop();
		logger.info("elapsed : {}", stopWatch);

		/** histogram 정렬 */
		Map<String, Integer> resultMap = new LinkedHashMap<>();
		result.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered(entry -> {
			resultMap.put(entry.getKey(), entry.getValue());
		});

		searchResult.setFinish(true);
		searchResult.setCount(totalCount.get());
		searchResult.setDate(start.format(defaultFormatter) + " ~ " + finish.plusNanos(1000L).format(defaultFormatter)); // custom 에서 minus 했던 것을 복구
		searchResult.setQueryTime(stopWatch.toString());
		searchResult.setHistogram(resultMap);
		searchResult.setConditionColumn(builder.getColumnSet());

		return searchResult;
	}

	/**
	 * 검색을 한다.
	 */
	public SearchResult search(SearchSupport searchSupport, long tableId, boolean export) {
		final SearchResult searchResult = makeSearchResult(searchSupport.getJobId());

		logger.info("search start : {}", searchSupport.getJobId());

		/** table 체크 */
		Map<String, ColumnInfo> columnInfoCache = ColumnInfoManager.COLUMNINFO_CACHE.get(tableId);
		if (null == columnInfoCache) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		/** 검색 범위 계산 */
		List<LocalDateTime> lists = SearchUtils.getBetweenDate(searchSupport.getSearchType(), searchSupport.getDateRange());
		LocalDateTime start = lists.get(0);
		LocalDateTime finish = lists.get(1);

		QueryBuilder builder = new QueryBuilder(searchSupport.getQuery(), columnInfoCache);
		Query subQuery;
		try {
			subQuery = builder.build();
		} catch (Exception e) {
			logger.error("query build exception", e);
			String[] args = { e.getMessage() };
			throw new UiValidationException("ALERT_SEARCH_CHECK_FAIL", args);
		}

		Sort sort = new Sort(new SortedNumericSortField("_date", Type.LONG, searchSupport.isReverse()));

		/** 쿼리를 만든다. */
		Builder rangeQueryBuilder = new BooleanQuery.Builder();
		if (searchSupport.getSearchType() == SearchType.CUSTOM || searchSupport.getSearchType() == SearchType.HOUR) { // 사용자 정의 영역에서만 range filter 처리를 한다.
			rangeQueryBuilder.add(LongPoint.newRangeQuery("_date", ParseUtils.makeTimeStamp(start), ParseUtils.makeTimeStamp(finish)), Occur.MUST);
		}

		rangeQueryBuilder.add(subQuery, Occur.MUST);
		Query rangeQuery = rangeQueryBuilder.build();

		Builder queryBuilder = new BooleanQuery.Builder();
		queryBuilder.add(subQuery, Occur.MUST);
		Query query = queryBuilder.build();
		logger.info("Query : {}", rangeQuery);

		/** 검색 대상을 가져온다. */
		List<String> targets = SearchUtils.searchTargetList(start, finish, false);

		/** range 대상을 찾는다. */
		String rangeKey1 = targets.get(0);
		String rangeKey2 = targets.get(targets.size() - 1);

		/** count 결과 map */
		Map<String, Integer> histogramMap = new LinkedHashMap<>();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		/** count 쿼리 시작 */
		logger.info("count Query start");
		long totalCount = 0L;
		AtomicInteger processCount = new AtomicInteger(0);
		
		for (String key : targets) {

			if (searchResult.isFinish()) {
				logger.debug("search process cancel : {}");
				break;
			}

			Optional<IndexSearchInfo> optional = indexManager.acquireSearcher(tableId, key);
			if (!optional.isPresent()) {
				continue;
			}

			IndexSearchInfo indexSearchInfo = optional.get();
			int count = 0;
			try {
				if (StringUtils.equals(rangeKey1, key) || StringUtils.equals(rangeKey2, key)) { // 해당 case는 rangeQuery 대상임.
					count = indexSearchInfo.getIndexSearcher().count(rangeQuery);

				} else {
					count = indexSearchInfo.getIndexSearcher().count(query);
				}
			} catch (Exception e) {
				logger.error("count search error", e);

			} finally {
				indexManager.releaseSearcher(indexSearchInfo);
			}

			histogramMap.put(key, count);
			totalCount += count;

			// 중지
			if (searchSupport.getLimit() > 0 && searchSupport.getLimit() < totalCount) {
				totalCount = searchSupport.getLimit();
				break;
			}
			
			/** 진행율 전송 */
			int currentCount = processCount.incrementAndGet();
			double percent = ((double) currentCount / targets.size()) * 100;
			webSocketManager.sendSearchStatus(new SearchStatus(searchSupport.getJobId(), totalCount, percent, false, "count"));
		}
		
		/** 진행율 전송 */
		webSocketManager.sendSearchStatus(new SearchStatus(searchSupport.getJobId(), totalCount, 100, false, "count"));

		/** 타겟 리스트 count를 넣어 준다. */
		searchResult.setCount(totalCount);
		searchResult.setHistogram(histogramMap);

		logger.info("count Query complete");
		logger.info("document Query start");

		/** 실제 검색 */
		List<SearchResultPaging> searchResultPagingList = findSearchPaging(histogramMap, searchSupport.getPageNo(), searchSupport.getPageSize());
		logger.info("document target : {}", searchResultPagingList);

		final Set<String> fieldToLoads = new HashSet<>();
		fieldToLoads.add("_text");
		fieldToLoads.add("_date");
		fieldToLoads.add("_address");
		fieldToLoads.add("_script");
		fieldToLoads.add("_scriptId");
		fieldToLoads.add("_size");

		/** 성능증가 목적의 코드 */
		boolean suggestSearchResult = false;
		if (searchResultPagingList.size() == 1) { // list가 1개일때만 작동한다.
			logger.info("suggest search start");

			String key = searchResultPagingList.get(0).getKey();
			IndexSearchInfo indexSearchInfo = indexManager.acquireSearcher(tableId, key).get();

			try {
				int suggestLimit = searchSupport.getPageSize() * searchSupport.getPageNo();
				int suggestOffset = suggestLimit - searchSupport.getPageSize();

				/** suggest 범위 찾기 */
				Optional<Query> suggest = findSuggestRange(indexSearchInfo.getIndexSearcher(), subQuery, start, finish, suggestLimit, searchSupport.isReverse());

				/** suggest 범위를 찾은 경우 */
				if (suggest.isPresent()) {
					Query suggestQuery = suggest.get();

					TopFieldCollector topFieldCollector = TopFieldCollector.create(sort, suggestLimit, false, false, false, true);
					indexSearchInfo.getIndexSearcher().search(suggestQuery, topFieldCollector);

					TopDocs topDocs = topFieldCollector.topDocs(suggestOffset, suggestLimit);

					for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
						Document document = indexSearchInfo.getIndexSearcher().doc(scoreDoc.doc, fieldToLoads);
						searchResult.getLists().add(makeResultToDocument(tableId, document, columnInfoCache, export));
					}

					suggestSearchResult = true;
				}

			} catch (Exception e) {
				logger.error("suggest search error", e);

			} finally {
				indexManager.releaseSearcher(indexSearchInfo);
			}
		}

		/** suggest 에서 범위를 찾지 못한 경우 처리 */
		if (!suggestSearchResult) {
			for (SearchResultPaging result : searchResultPagingList) {
				String key = result.getKey();

				IndexSearchInfo indexSearchInfo = indexManager.acquireSearcher(tableId, key).get();
				if (null == indexSearchInfo) {
					continue;
				}

				logger.debug("document Query start1");
				try {
					TopFieldCollector topFieldCollector = TopFieldCollector.create(sort, result.getLimit(), false, false, false, true);

					if (StringUtils.equals(rangeKey1, key) || StringUtils.equals(rangeKey2, key)) { // 해당 case는 rangeQuery 대상임.
						indexSearchInfo.getIndexSearcher().search(rangeQuery, topFieldCollector);

					} else {
						indexSearchInfo.getIndexSearcher().search(query, topFieldCollector);
					}
					logger.debug("document Query start2");

					TopDocs topDocs = topFieldCollector.topDocs(result.getOffset(), result.getLimit() - result.getOffset());
					for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
						Document document = indexSearchInfo.getIndexSearcher().doc(scoreDoc.doc, fieldToLoads);
						searchResult.getLists().add(makeResultToDocument(tableId, document, columnInfoCache, export));
					}
					logger.debug("document Query start3");
					
					/** 진행율 전송 */
					double percent = ((double) result.getOffset() / result.getLimit()) * 100;
					webSocketManager.sendSearchStatus(new SearchStatus(searchSupport.getJobId(), totalCount, percent, false, "getDocument"));

				} catch (Exception e) {
					logger.error("search error!!", e);

				} finally {
					indexManager.releaseSearcher(indexSearchInfo);
				}
			};
		}
		
		/** 진행율 전송 */
		webSocketManager.sendSearchStatus(new SearchStatus(searchSupport.getJobId(), totalCount, 100, false, "getDocument"));

		logger.info("document Query complete");

		stopWatch.stop();
		searchResult.setFinish(true);
		searchResult.setDate(start.format(defaultFormatter) + " ~ " + finish.plusNanos(1000L).format(defaultFormatter)); // custom 에서 minus 했던 것을 복구
		searchResult.setQueryTime(stopWatch.toString());
		searchResult.setConditionColumn(builder.getColumnSet());

		return searchResult;
	}

	/**
	 * 검색을 취소한다.
	 */
	public boolean cancel(SearchSupport searchSupport) {
		final SearchResult searchResult = makeSearchResult(searchSupport.getJobId());
		searchResult.setFinish(true);
		logger.info("[search Cancel] jobId = " + searchSupport.getJobId() + ", limitRows = " + searchResult.getCount());
		return true;
	}

	public SearchResultBean makeResultToDocument(long tableId, Document document, Map<String, ColumnInfo> columnInfoCache, boolean export) throws NoSuchMethodException, ScriptException {
		SearchResultBean searchResultBean = new SearchResultBean();

		searchResultBean.setText(document.get("_text"));
		String dateObj = document.get("_date");
		String senderIp = document.get("_address");
		if (!StringUtils.isEmpty(dateObj)) {
			searchResultBean.setDate(new Timestamp(NumberUtils.toLong(dateObj)).toLocalDateTime());
		}

		final Map<String, Object> result = new LinkedHashMap<>();

		ScriptInfo scriptInfo;
		String scriptIdText = document.get("_scriptId");
		if (null != scriptIdText) {
			long scriptId = NumberUtils.toLong(scriptIdText);
			scriptInfo = ScriptInfoManager.getScriptInfoCache(tableId, scriptId);
		} else {
			/** 하위호환 */
			String scriptName = document.get("_script");
			scriptInfo = ScriptInfoManager.getScriptInfoCache(tableId, scriptName);
		}

		if (null != scriptInfo) {
			searchResultBean.setScriptId(scriptInfo.getId());

			/** export 인경우 미리 해석하지 않는다. (메모리이슈) */
			if (!export) {
				if (!StringUtils.isEmpty(searchResultBean.getText())) {
					LogBean logBean = new LogBean(tableId, searchResultBean.getScriptId(), searchResultBean.getText(), searchResultBean.getAddress());
					logBean.setReceiveDate(searchResultBean.getDate().atZone(ZoneId.systemDefault()));

					scriptInfo.getInvocable().invokeFunction("parseLog", logBean, false);

					int keySize = logBean.getKeyList().size();
					for (int i = 0; i < keySize; i++) {

						String fieldName = logBean.getKey(i);
						String logText = logBean.getValue(i);

						result.put(fieldName, logText);
					}
				}
			}
		}

		/** 예외처리 */
		result.entrySet().forEach(entry -> {
			ColumnInfo columnInfo = columnInfoCache.get(entry.getKey());
			if (null == columnInfo) {
				return;
			}

			/** 국가 타입의 경우 예외처리 해줌 */
			if (columnInfo.getLogFieldType() == LogFieldType.COUNTRY) {
				String country = DocumentMapper.getCountryCode((String) entry.getValue());
				result.put(entry.getKey(), country);

			} else if (columnInfo.getLogFieldType() == LogFieldType.DATETIME) {
				long time = NumberUtils.toLong((String) entry.getValue());
				if (time < 1) {
					result.put(entry.getKey(), "");
				} else {
					result.put(entry.getKey(), DateUtils.parseDate(new Timestamp(NumberUtils.toLong((String) entry.getValue())).toLocalDateTime()));
				}

			}
		});

		result.put("date", searchResultBean.getDate().format(defaultFormatter2));
		result.put("size", document.get("_size"));
		result.put("address", senderIp);

		searchResultBean.setData(result);
		searchResultBean.setAddress(senderIp);

		return searchResultBean;
	}

	/**
	 * 실제 목록을 가져올 검색대상
	 */
	private List<SearchResultPaging> findSearchPaging(Map<String, Integer> resultMap, int pageNo, int pageSize) {
		int startNum = pageSize * (pageNo - 1);
		int currentCount = 0;
		int requestCount = pageSize;

		List<SearchResultPaging> result = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : resultMap.entrySet()) {
			int count = entry.getValue();
			if (count == 0) {
				continue;
			}

			currentCount += count;

			if (startNum < currentCount) { // 시작시점 찾기
				int offset = count - (currentCount - startNum);
				int limit = offset + requestCount;

				if (offset < 0) { // offset 음수 예외처리
					offset = 0;
				}

				if (limit > count) { // count max 처리
					limit = count;
				}

				if (limit <= 0) { // limit 이 0이하인 경우 그만.
					break;
				}
				result.add(new SearchResultPaging(entry.getKey(), offset, limit));
			}
		}
		return result;
	}

	private Optional<Query> findSuggestRange(IndexSearcher searcher, Query subQuery, LocalDateTime start, LocalDateTime finish, int targetCount, boolean reverse) throws IOException {
		boolean empty = false;
		long[] seconds = null;

		if (StringUtils.equals(subQuery.toString(), "*:*")) {
			seconds = new long[61];
			seconds[0] = 10L;
			for (int i = 1; i < 61; i++) {
				seconds[i] = i * 60;
			}

			empty = true;
		} else if (StringUtils.contains(subQuery.toString(), "*")) {
			seconds = new long[] { 10L, 60L, 600L, 3000L };

		} else {
			seconds = new long[11];
			seconds[0] = 10L;
			for (int i = 1; i < 10; i++) {
				seconds[i] = i * 360;
			}
		}

		for (long second : seconds) {
			LocalDateTime start2 = null;
			LocalDateTime finish2 = null;

			if (reverse) {
				start2 = finish.minusSeconds(second);
				finish2 = finish;
			} else {
				start2 = start;
				finish2 = start.plusSeconds(second);
			}

			if (start2.isBefore(start)) {
				start2 = start;
			}

			if (finish2.isAfter(finish)) {
				finish2 = finish;
			}

			Query suggestQuery = LongPoint.newRangeQuery("_date", ParseUtils.makeTimeStamp(start2), ParseUtils.makeTimeStamp(finish2));
			if (!empty) {
				Builder queryBuilder = new BooleanQuery.Builder();
				queryBuilder.add(suggestQuery, Occur.MUST);
				queryBuilder.add(subQuery, Occur.MUST);

				suggestQuery = queryBuilder.build();
			}

			int count = searcher.count(suggestQuery);
			if (count >= targetCount) {
				logger.info("suggest search find! {}s => {} ~ {}", second, finish.minusSeconds(second), finish);

				return Optional.of(suggestQuery);
			}
		}

		return Optional.empty();
	}

	private SearchResult makeSearchResult(long jobId) {
		stopSearch(jobId);

		try {
			return jobCache.get(jobId);
		} catch (ExecutionException e) {
			return new SearchResult(jobId);
		}
	}

	private void stopSearch(long jobId) {
		jobCache.invalidate(jobId);
	}

}
