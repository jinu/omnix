package com.omnix.manager.statistics;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.Query;
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
import com.omnix.manager.search.SearchType;
import com.omnix.manager.statistics.collector.AggregateCollector;
import com.omnix.manager.statistics.collector.GroupByNCollector;
import com.omnix.manager.statistics.collector.GroupByStatCollector;
import com.omnix.manager.statistics.collector.GroupByTrendCollector;
import com.omnix.manager.statistics.collector.GroupByTrendMinCollector;
import com.omnix.manager.statistics.collector.StatCollector;
import com.omnix.manager.statistics.collector.StatMinCollector;
import com.omnix.manager.statistics.collector.TimeStatCollector;
import com.omnix.manager.websocket.SearchStatus;
import com.omnix.manager.websocket.WebSocketManager;
import com.omnix.util.ParseUtils;
import com.omnix.util.QueryBuilder;
import com.omnix.util.SearchUtils;

@Service
public class IndexStatisticsManager {
	@Autowired
	private Config config;
	@Autowired
	private IndexManager indexManager;
	@Autowired
	private WebSocketManager webSocketManager;

	/** JobId Cache **/
	private LoadingCache<Long, AggregateResult> jobCache;

	/** Logger */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@PostConstruct
	public void init() {
		logger.info("search cache init. max hour : {}", config.getSearchCacheMaxHour());
		jobCache = CacheBuilder.newBuilder().refreshAfterWrite(config.getSearchCacheMaxHour(), TimeUnit.HOURS).removalListener(new RemovalListener<Long, AggregateResult>() {
			@Override
			public void onRemoval(RemovalNotification<Long, AggregateResult> entry) {
				entry.getValue().setFinish(true);
			}

		}).build(CacheLoader.<Long, AggregateResult>from(jobId -> {
			AggregateResult aggregateResult = new AggregateResult(jobId);
			return aggregateResult;
		}));
	}

	public AggregateResult statistics(AggregateSupport aggregateSupport, long tableId) {
		final AggregateResult aggregateResult = makeAggregateResult(aggregateSupport.getJobId());
		logger.info("statistics start : {}", aggregateResult.getJobId());

		/** table 체크 */
		Map<String, ColumnInfo> columnInfoCache = ColumnInfoManager.COLUMNINFO_CACHE.get(tableId);
		if (null == columnInfoCache) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		/** 검색 범위 계산 */
		List<LocalDateTime> lists = SearchUtils.getBetweenDate(aggregateSupport.getSearchType(), aggregateSupport.getDateRange());
		LocalDateTime start = lists.get(0);
		LocalDateTime finish = lists.get(1);

		QueryBuilder builder = new QueryBuilder(aggregateSupport.getQuery(), columnInfoCache);
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
		if (aggregateSupport.getSearchType() == SearchType.CUSTOM || aggregateSupport.getSearchType() == SearchType.HOUR) { // 사용자 정의 영역에서만 range filter 처리를 한다.
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

		AggregateCollector collectorTemp = null;

		List<ColumnInfo> columnInfos = new ArrayList<>();

		switch (aggregateSupport.getAggregateType()) {
		case GROUP_BY_STAT:

			for (String column : aggregateSupport.getColumns()) {
				columnInfos.add(ColumnInfoManager.getColumnInfoCache(tableId, column));
			}

			collectorTemp = new GroupByStatCollector(columnInfos, aggregateSupport.getLimit(), aggregateSupport.getGroupBySort());

			break;

		case SINGLE_STAT:
			for (String column : aggregateSupport.getColumns()) {
				ColumnInfo statColumnInfo = ColumnInfoManager.getColumnInfoCache(tableId, column);
				columnInfos.add(statColumnInfo);
			}

			/** 같은 시간 범위이면 statMinCollector 호출 */
			if (targets.size() <= 2) {
				collectorTemp = new StatMinCollector(columnInfos, aggregateSupport.getLimit());
			} else {
				collectorTemp = new StatCollector(columnInfos, aggregateSupport.getLimit());
			}

			break;

		case TIME_STAT:
			for (String column : aggregateSupport.getColumns()) {
				columnInfos.add(ColumnInfoManager.getColumnInfoCache(tableId, column));
			}

			collectorTemp = new TimeStatCollector(columnInfos, aggregateSupport.getLimit());

			break;

		case GROUP_BY_TREND:
			for (String column : aggregateSupport.getColumns()) {
				columnInfos.add(ColumnInfoManager.getColumnInfoCache(tableId, column));
			}

			if (targets.size() <= 2) {
				collectorTemp = new GroupByTrendMinCollector(columnInfos, aggregateSupport.getLimit(), aggregateSupport.getGroupBySort());
			} else {
				collectorTemp = new GroupByTrendCollector(columnInfos, aggregateSupport.getLimit(), aggregateSupport.getGroupBySort());
			}

			break;
		case GROUP_BY_N_STAT:

			for (String column : aggregateSupport.getColumns()) {
				columnInfos.add(ColumnInfoManager.getColumnInfoCache(tableId, column));
			}

			List<ColumnInfo> keyList = new ArrayList<>();
			ColumnInfo statColumn = null;

			/** 마지막 컬럼 정보 */
			ColumnInfo lastColumnInfo = columnInfos.get(columnInfos.size() - 1);

			/** 모든 필드가 key인 경우 */
			if (lastColumnInfo == null) {
				keyList = columnInfos.stream().limit(columnInfos.size() - 1).collect(Collectors.toList());
			} else {
				if (lastColumnInfo.getLogFieldType().isStatField()) {
					statColumn = lastColumnInfo;
					keyList = columnInfos.stream().limit(columnInfos.size() - 1).collect(Collectors.toList());
				} else {
					keyList = columnInfos.stream().collect(Collectors.toList());
				}
			}

			collectorTemp = new GroupByNCollector(keyList, statColumn, aggregateSupport.getLimit(), aggregateSupport.getGroupBySort());

			break;
		}

		final AggregateCollector collector = collectorTemp;

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		AtomicInteger processCount = new AtomicInteger(0);
		targets.forEach(dateKey -> {
			if (aggregateResult.isFinish()) {
				logger.debug("statistics process cancel : {}", aggregateResult.getJobId());
				return;
			}

			Optional<IndexSearchInfo> optional = indexManager.acquireSearcher(tableId, dateKey);
			optional.ifPresent(indexSearchInfo -> {
				try {
					collector.startHistogramCollect(true);

					if (StringUtils.equals(rangeKey1, dateKey) || StringUtils.equals(rangeKey2, dateKey)) { // 해당 case는 rangeQuery 대상임.
						indexSearchInfo.getIndexSearcher().search(rangeQuery, collector);
					} else {
						indexSearchInfo.getIndexSearcher().search(query, collector);
					}
					collector.merge();
					collector.endHistogramCollect(dateKey, start, finish);

					/** limit 에 걸리면 중지 */
					if (collector.isFull()) {
						aggregateResult.setFinish(true);
						return;
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
			webSocketManager.sendSearchStatus(new SearchStatus(aggregateResult.getJobId(), 0, percent, false, "statistics"));

		});

		stopWatch.stop();
		logger.info("elapsed : {}", stopWatch);

		aggregateResult.setFinish(true);
		aggregateResult.setDate(start.format(defaultFormatter) + " ~ " + finish.plusNanos(1000L).format(defaultFormatter));
		aggregateResult.setQueryTime(stopWatch.toString());
		aggregateResult.setCount(collector.getTotalCount());
		aggregateResult.setResult(collector.getValues(aggregateSupport.getTopCount()));
		aggregateResult.setHistogramResult(collector.getHistogramMap());

		/** 값 column의 총합을 넣는다. */
		aggregateResult.setColumnValueList(collector.getTotalValues());

		return aggregateResult;
	}

	private AggregateResult makeAggregateResult(long jobId) {
		stopSearch(jobId);

		try {
			return jobCache.get(jobId);
		} catch (ExecutionException e) {
			return new AggregateResult(jobId);
		}
	}

	private void stopSearch(long jobId) {
		jobCache.invalidate(jobId);
	}

	/**
	 * 검색을 취소한다.
	 */
	public boolean cancel(AggregateSupport aggregateSupport) {
		final AggregateResult aggregateResult = makeAggregateResult(aggregateSupport.getJobId());
		aggregateResult.setFinish(true);

		logger.info("[statistics Cancel] jobId = " + aggregateResult.getJobId() + ", limitRows = " + aggregateResult.getCount());
		return true;
	}

}
