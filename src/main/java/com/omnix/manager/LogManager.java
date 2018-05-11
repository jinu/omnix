package com.omnix.manager;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.InetAddressPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.omnix.config.Config;
import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.ColumnInfoManager;
import com.omnix.manager.parser.DocumentMapper;
import com.omnix.manager.parser.ScriptInfo;
import com.omnix.manager.parser.ScriptInfoManager;
import com.omnix.manager.recieve.LogBean;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

@Service
public class LogManager {
	@Autowired
	private Config config;
	@Autowired
	private IndexManager indexManager;
	@Autowired
	private DocumentMapper documentMapper;

	private Disposable logDisposable;

	/** 로그 이벤트 발행 */
	public static final PublishSubject<LogBean> PUBSUB = PublishSubject.create();

	public static final LongAdder TOTAL_LOG_COUNT = new LongAdder();
	public static final LongAdder PROCESS_LOG_COUNT = new LongAdder();
	public static final LongAdder WRITE_LOG_COUNT = new LongAdder();
	public static final LongAdder ERROR_LOG_COUNT = new LongAdder();
	public static final LongAdder IGNORE_LOG_COUNT = new LongAdder();

	public static final String SCRIPT_KEY_SEPERATOR = "_";
	private static final DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
	private static final DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("mm");
	
	private ExecutorService writerExcutor;
	
	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public void init() {
		
		logger.info("create writer thread : {}", config.getWriterThread());
		writerExcutor = Executors.newFixedThreadPool(config.getWriterThread());
		
		logger.info("create pubsub");
		logDisposable = PUBSUB.buffer(config.getLogBufferMs(), TimeUnit.SECONDS, config.getLogBufferCount()).subscribe(getLogBeanConsumer());
	}

	/**
	 * 로그 처리 Consumer
	 * 
	 * @return Consumer<List<LogBean>>
	 */
	private Consumer<List<LogBean>> getLogBeanConsumer() {
		Consumer<List<LogBean>> consumer = list -> {

			if (list.size() == 0) {
				return;
			}

			writerExcutor.submit(() -> {
				try {

					/** {tableId : {dateKey : values}} */
					Map<Long, Map<String, List<LogBean>>> map = new LinkedHashMap<>();

					String beforeIndexKey = "";
					long beforeTimestamp = 0L;
					for (LogBean logBean : list) {

						if (!logBean.isParseComplete()) {
							/** 로그 해석 */
							try {
								boolean flag = parseLog(logBean);

								if (!flag) {
									IGNORE_LOG_COUNT.increment();
									continue;
								}

							} catch (Exception e) {
								ERROR_LOG_COUNT.increment();
								continue;
//								logger.error("{}", logBean);
//								logger.error("parse fail", e);
							}
						}

						long timestamp = logBean.getTimestamp();

						/** timestamp 값이 같은 경우 format을 가져오지 않는다. */
						String indexKey;
						if (beforeTimestamp == timestamp) {
							indexKey = beforeIndexKey;
						} else {
							ZonedDateTime _date = logBean.getReceiveDate();
							indexKey = _date.format(hourFormatter);

							beforeIndexKey = indexKey;
							beforeTimestamp = timestamp;
						}

						Map<String, List<LogBean>> map2 = map.get(logBean.getTableId());
						if (null == map2) {
							map2 = new LinkedHashMap<>();
							map.put(logBean.getTableId(), map2);
						}

						List<LogBean> subList = map2.get(indexKey);

						/** list가 존재하지 않을 경우 최초 생성 */
						if (null == subList) {
							subList = new ArrayList<>();
							map2.put(indexKey, subList);
						}

						/** 해당 list에 document add */
						subList.add(logBean);
					}

					map.entrySet().forEach(entry1 -> {
						long tableId = entry1.getKey();

						entry1.getValue().entrySet().forEach(entry2 -> {
							String indexKey = entry2.getKey();

							List<Document> lists = new ArrayList<>();

							/** 로그를 해석한다. */
							for (LogBean logBean : entry2.getValue()) {
								Document document = generateDocument(logBean);
								if (null != document) {
									lists.add(document);
								}
							}

							try {
								indexManager.addIndex(tableId, indexKey, lists);
								WRITE_LOG_COUNT.add(lists.size());

							} catch (Exception e) {
								logger.error("writer error", e);
							}
						});

					});

					map = null;

				} finally {
					PROCESS_LOG_COUNT.add(list.size());
				}

			});

		};

		return consumer;
	}

	/**
	 * 로그를 해석한다.
	 */
	public boolean parseLog(LogBean logBean) throws NoSuchMethodException, ScriptException {
		logBean.setIndexFlag(true);

		ScriptInfo scriptInfo = ScriptInfoManager.getScriptInfoCache(logBean.getTableId(), logBean.getScriptId());
		if (null == scriptInfo) {
			return false;
		}

		Invocable engine = scriptInfo.getInvocable();
		engine.invokeFunction("parseLog", logBean, true);

		if (logBean.isIgnore()) {
			return false;
		}

		logBean.setParseComplete(true);
		return true;
	}

	/**
	 * 로그를 document로 바꾼다.
	 */
	public Document generateDocument(LogBean logBean) {
		Document document = new Document();

		Map<String, ColumnInfo> columnInfoMap = ColumnInfoManager.getColumnInfoCache(logBean.getTableId());

		try {

			long beforeTimeStampSec = 0L;
			BytesRef beforeDate = null; // yyyyMMddHHmmss

			/** 필드를 얻어와 document를 만든다. */
			int keySize = logBean.getKeyList().size();
			for (int i = 0; i < keySize; i++) {

				String fieldName = logBean.getKey(i);
				String logText = logBean.getValue(i);

				ColumnInfo columnInfo = columnInfoMap.get(fieldName);

				if (null == columnInfo) {
					continue;
				}

				/** search field */
				try {
					List<Field> searchFields = documentMapper.makeFieldBySearch(columnInfo, logText);
					if (null != searchFields) {
						for (Field field : searchFields) {
							document.add(field);
						}
					}

					/** statistics field */
					List<Field> statisticsField = documentMapper.makeFieldByStatistics(columnInfo, logText);
					if (null != statisticsField) {
						for (Field field : statisticsField) {
							document.add(field);
						}
					}

				} catch (Exception e) {
					logger.debug("document mapper error", e);
				}

			}

			long date = logBean.getTimestamp();

			document.add(new LongPoint("_date", date));
			document.add(new SortedNumericDocValuesField("_date", date));
			document.add(new StoredField("_date", date));

			/** 초단위 값 저장 */
			long date2 = (long) (date / 1000);

			if (date2 != beforeTimeStampSec) {
				ZonedDateTime zonedDateTime = logBean.getReceiveDate();
				beforeDate = new BytesRef(zonedDateTime.format(datetimeFormatter));
			}

			document.add(new SortedDocValuesField("_date2", beforeDate));

			/** 원본 로그를 resultMap으로 받는 경우 처리 */
			if (logBean.isRawlog()) {
				document.add(new StoredField("_text", logBean.getText()));
			}

			/** store 옵션인 경우는 script 명을 저장할 필요가 없음. */
			document.add(new StoredField("_scriptId", logBean.getScriptId()));

			/** 통합 검색용 index 처리 */
			String message = logBean.getMessageList();
			if (!StringUtils.isEmpty(message)) {
				document.add(new TextField("_message", StringUtils.lowerCase(message), Store.NO));
			}

			int size = logBean.getText().length();
			document.add(new LongPoint("_size", size));
			document.add(new NumericDocValuesField("_size", size));
			document.add(new StoredField("_size", size));

			if (!StringUtils.isEmpty(logBean.getRemoteAddr())) {
				document.add(new InetAddressPoint("_address", InetAddress.getByName(logBean.getRemoteAddr())));
				document.add(new StringField("_address", logBean.getRemoteAddr(), Store.YES));
				document.add(new SortedDocValuesField("_address", new BytesRef(logBean.getRemoteAddr())));
			}

			return document;
		} catch (Exception e) {
			logger.error("log parse error", e);
			return null;
		}

	}
	
	public static void sendLog(LogBean logBean) {
		PUBSUB.onNext(logBean);
		incrementLogCount();
	}

	public static void incrementLogCount() {
		TOTAL_LOG_COUNT.increment();
	}
	
	public static void addLogCount(long count) {
		TOTAL_LOG_COUNT.add(count);
	}

	public static long getQueueCount() {
		return TOTAL_LOG_COUNT.longValue() - PROCESS_LOG_COUNT.longValue();
	}
	
	public boolean awaitQueue() {
		int waitCount = 0;
		boolean flag = true;
		
		while(true) {
			if (waitCount > 60) {
				logger.error("queue full!!");
				flag = false;
				break;
			}
			
			if (getQueueCount() > config.getLogBufferLimit()) {
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {
				}
			} else {
				break;
			}
			
			waitCount++;
		}
		
		return flag;
	}
	
	@Scheduled(fixedRate = 5000L)
	public void logging() {
		logger.info("total : {}, current : {}, write: {}, ignore : {}, error : {}", TOTAL_LOG_COUNT.longValue(), PROCESS_LOG_COUNT.longValue(), WRITE_LOG_COUNT.longValue(), IGNORE_LOG_COUNT.longValue(), ERROR_LOG_COUNT.longValue());
	}

	@PreDestroy
	public void destroy() {
		logDisposable.dispose();
	}
}
