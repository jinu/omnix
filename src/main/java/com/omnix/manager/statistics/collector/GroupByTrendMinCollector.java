package com.omnix.manager.statistics.collector;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.statistics.DocumentStatUtils;
import com.omnix.manager.statistics.DocumentsStat;
import com.omnix.manager.statistics.GroupBySort;

public class GroupByTrendMinCollector extends AggregateCollector {
	/** group by 가 동작할 필드명 */
	private final String keyField;
	private List<ColumnInfo> columnInfos;

	private long count = 0;
	private final GroupBySort groupBySort;

	private long limit;
	private boolean full;
	private boolean countOnly;

	/** SortedDocValues */
	private SortedDocValues keyIndex;
	private List<NumericDocValues> numericIndexes = new ArrayList<>();
	private SortedDocValues timeIndex; // timestamp
	/** slot */
	private List<DocumentsStat[][]> documentsStatsSlots = new ArrayList<>();
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	/** [dateOrd][countOrd] */
	private int[][] dateCountSlot;

	/** <날짜, <key, DocumentsStat[]>> */
	private Map<String, Map<String, List<DocumentsStat>>> histogramMap = new HashMap<>();
	/** <날짜, <key, DocumentsStat[]>> */
	private Map<String, Map<String, List<DocumentsStat>>> resultHistogramMap = new HashMap<>();
	/** Logger */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * group by trend 생성자
	 * 
	 * @param columnInfos
	 *            [key] 필드, [stats] field
	 * @param limit
	 *            [limit 값]
	 */
	public GroupByTrendMinCollector(List<ColumnInfo> columnInfos, long limit, GroupBySort groupBySort) {
		this.keyField = columnInfos.get(0).getName(); // 0번은 항상 keyField
		this.limit = limit;

		/** columnInfo 가 null 인 경우 count만 collect한다. */
		if (columnInfos.size() == 1 || columnInfos.get(1) == null) {
			this.countOnly = true;
		} else {
			List<ColumnInfo> list = new ArrayList<>();
			for (int i = 1; i < columnInfos.size(); i++) {
				list.add(columnInfos.get(i));
			}

			this.columnInfos = list;
		}
		this.groupBySort = groupBySort;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		merge();

		try {
			/** index 가 변경될때 처리함. */
			keyIndex = DocValues.getSorted(context.reader(), keyField);
			/** time 값 */
			timeIndex = DocValues.getSorted(context.reader(), "_date2");
		} catch (IllegalStateException e) {
			/** statistics indexing 변경이 일어난 경우, 해당 context는 건너 뛴다. */
			keyIndex = null;
			return;
		}

		/** unique 한 key들의 count 값을 가져온다. */
		int keyCount = keyIndex.getValueCount();
		int timeCount = timeIndex.getValueCount();

		if (countOnly) {
			dateCountSlot = new int[timeCount][keyCount];
		} else {
			for (int i = 0; i < columnInfos.size(); i++) {
				ColumnInfo columnInfo = columnInfos.get(i);

				NumericDocValues numericIndex = null;
				DocumentsStat[][] documentsStatsSlot = null;
				try {
					numericIndex = DocValues.getNumeric(context.reader(), columnInfo.getName());
					documentsStatsSlot = new DocumentsStat[timeCount][keyCount];

					for (int j = 0; j < documentsStatsSlot.length; j++) {
						for (int k = 0; k < documentsStatsSlot[j].length; k++) {
							documentsStatsSlot[j][k] = new DocumentsStat();
						}
					}

				} catch (IllegalStateException e) {
					logger.info("reader error.");
				}

				numericIndexes.add(numericIndex);
				documentsStatsSlots.add(documentsStatsSlot);
			}
		}
		logger.debug("slotSize : {}", keyCount);
	}

	@Override
	public boolean needsScores() {
		return false;
	}

	@Override
	public void collect(int doc) throws IOException {
		if (full || (limit > 0 && count >= limit)) {
			this.full = true;
			return;
		}

		++count;
		if (countOnly) {
			doAccumulateOnlyCount(doc);
		} else {
			for (int i = 0; i < columnInfos.size(); i++) {
				doAccumulate(doc, columnInfos.get(i), numericIndexes.get(i), documentsStatsSlots.get(i));
			}

		}
	}

	public void doAccumulateOnlyCount(int doc) throws IOException {
		if (keyIndex == null || timeIndex == null) {
			return;
		}

		boolean keyFlag = keyIndex.advanceExact(doc);
		boolean timeFlag = timeIndex.advanceExact(doc);

		if (!keyFlag || !timeFlag) {
			return;
		}

		int keyOrd = keyIndex.ordValue();
		int timeOrd = timeIndex.ordValue();

		/** time별 key 값을 카운트 한다. */
		dateCountSlot[timeOrd][keyOrd]++;
	}

	public void doAccumulate(int doc, ColumnInfo columnInfo, NumericDocValues numericIndex, DocumentsStat[][] documentsStatsSlot) throws IOException {
		if (keyIndex == null || timeIndex == null || numericIndexes.size() == 0) {
			return;
		}

		boolean keyFlag = keyIndex.advanceExact(doc);
		boolean timeFlag = timeIndex.advanceExact(doc);

		if (!keyFlag || !timeFlag) {
			return;
		}

		boolean numericFlag = numericIndex.advanceExact(doc);
		double val = 0L;
		if (numericFlag) {
			if (columnInfo.getLogFieldType() == LogFieldType.DOUBLE) {
				val = Double.longBitsToDouble(numericIndex.longValue());
			} else {
				val = numericIndex.longValue();
			}
		}

		int keyOrd = keyIndex.ordValue();
		int timeOrd = timeIndex.ordValue();

		DocumentsStat documentsStat = documentsStatsSlot[timeOrd][keyOrd];

		documentsStat.addCount(1L);
		documentsStat.addSum(val);
		documentsStat.addPow2sum(Math.pow(val, 2));
		documentsStat.setFlag(true);

		if (val > documentsStat.getMax()) {
			documentsStat.setMax(val);
		}

		if (val < documentsStat.getMin()) {
			documentsStat.setMin(val);
		}
	}

	@Override
	public void merge() throws IOException {
		if (countOnly) {
			if (null == dateCountSlot) {
				return;
			}

			/** time 길이 만큼 for문을 돈다. */
			for (int i = 0; i < dateCountSlot.length; i++) {
				String dateKey = timeIndex.lookupOrd(i).utf8ToString(); // 시간값

				// <groupkey, document list>
				Map<String, List<DocumentsStat>> dataMap = histogramMap.get(dateKey);

				if (null == dataMap) {
					dataMap = new HashMap<>();
					histogramMap.put(dateKey, dataMap);
				}

				/** 해당 time의 존재하는 group key 만큼 돈다. */
				for (int j = 0; j < dateCountSlot[i].length; j++) {
					long count = dateCountSlot[i][j];

					if (count > 0) {
						String groupKey = keyIndex.lookupOrd(j).utf8ToString(); // group key

						// documents List
						List<DocumentsStat> docList = dataMap.get(groupKey);
						if (null == docList) {
							DocumentsStat documentsStat = new DocumentsStat();
							documentsStat.setCount(count);
							documentsStat.setKey(groupKey);

							dataMap.put(groupKey, Arrays.asList(documentsStat));
						} else {
							docList.get(0).addCount(count);
						}
					}
				}
			}
		} else {
			if (CollectionUtils.isEmpty(documentsStatsSlots)) {
				return;
			}

			/** 컬럼 별로 document를 합쳐준다. */
			for (int i = 0; i < columnInfos.size(); i++) {

				DocumentsStat[][] documentArray = documentsStatsSlots.get(i);

				/** time 길이 만큼 for문을 돈다. */
				for (int j = 0; j < documentArray.length; j++) {

					String dateKey = timeIndex.lookupOrd(j).utf8ToString(); // 시간값
					// <groupkey, document list>
					Map<String, List<DocumentsStat>> dataMap = histogramMap.get(dateKey);

					if (null == dataMap) {
						dataMap = new HashMap<>();
						histogramMap.put(dateKey, dataMap);
					}

					/** 해당 time의 존재하는 group key 만큼 돈다. */
					for (int k = 0; k < documentArray[j].length; k++) {
						DocumentsStat documentsStat = documentArray[j][k];
						if (documentsStat.isFlag()) {
							String groupKey = keyIndex.lookupOrd(k).utf8ToString(); // group key
							List<DocumentsStat> docList = dataMap.get(groupKey);

							if (null == docList) {
								docList = new ArrayList<>();
								dataMap.put(groupKey, docList);
							}

							if (docList.size() == i) {
								documentsStat.setKey(groupKey);

								documentsStat.makeAvg();
								documentsStat.makeStdev();

								docList.add(documentsStat);
							} else {
								DocumentsStat oldDocumentStat = docList.get(i);

								oldDocumentStat.addCount(documentsStat.getCount());
								oldDocumentStat.addSum(documentsStat.getSum());
								oldDocumentStat.addPow2sum(documentsStat.getPow2sum());
								oldDocumentStat.makeAvg();
								oldDocumentStat.makeStdev();

								if (documentsStat.getMax() > oldDocumentStat.getMax()) {
									oldDocumentStat.setMax(documentsStat.getMax());
								}

								if (documentsStat.getMin() < oldDocumentStat.getMin()) {
									oldDocumentStat.setMin(documentsStat.getMin());
								}
							}
						}
					}
				}
			}
		}
		numericIndexes.clear();
		documentsStatsSlots.clear();

		dateCountSlot = null;
		keyIndex = null;
		timeIndex = null;
	}

	@Override
	public void startHistogramCollect(boolean flag) {
		histogramCollectFlag = flag;
		histogramMap = new HashMap<>();
	}

	@Override
	public void endHistogramCollect(String indexKey, LocalDateTime start, LocalDateTime finish) {
		if (histogramCollectFlag) {
			for (int i = 0; i < 60; i++) {
				StringBuilder time = new StringBuilder();
				StringBuilder timeStr = new StringBuilder();
				time.append(indexKey);

				if (i < 10) {
					timeStr.append("0").append(i);
					time.append(timeStr);
				} else {
					timeStr.append(i);
					time.append(i);
				}

				Long rangeStart = Long.parseLong(formatter.format(start));
				Long rangeFinish = Long.parseLong(formatter.format(finish.minusNanos(1000L)));

				Long timeNumber = Long.parseLong(time.toString());

				if (rangeStart <= timeNumber && rangeFinish >= timeNumber) {
					Map<String, List<DocumentsStat>> data = histogramMap.get(String.valueOf(timeStr.toString()));

					resultHistogramMap.put(time.toString(), data == null ? new HashMap<>() : data);
				}
			}
		}
		histogramMap = null;
	}

	/** group key 별 총합을 종합하여 준다. */
	@Override
	public List<String> getValues(int count) {
		Map<String, List<DocumentsStat>> totalData = new HashMap<>(); // key별 Map list

		resultHistogramMap.values().stream().forEach(dataMap -> {
			// count 만 구할 경우
			if (countOnly) {
				/** document의 list들을 꺼내온다. */
				dataMap.values().stream().forEach(documentsList -> {
					/** 모든 document를 꺼내온다. */
					documentsList.stream().forEach(document -> {
						String key = document.getKey(); // group 의 값
						/** 결과 본에 들고있는 데이터 */
						List<DocumentsStat> lists = totalData.get(key);

						/** list들의 값을 종합한다. */
						if (null == lists) {
							DocumentsStat documentStat = new DocumentsStat();
							documentStat.setCount(document.getCount());
							documentStat.setKey(key);

							totalData.put(key, Arrays.asList(documentStat));
						} else {
							lists.get(0).addCount(document.getCount());
						}
					});
				});
			} else {
				dataMap.entrySet().stream().forEach(data -> {
					String key = data.getKey(); // group key
					List<DocumentsStat> documentList = data.getValue(); // document list

					if (documentList.isEmpty()) {
						return;
					}

					for (int i = 0; i < columnInfos.size(); i++) {
						DocumentsStat documentsStat = documentList.get(i);
						List<DocumentsStat> oldDocumentStats = totalData.get(key);
						if (null == oldDocumentStats) {
							oldDocumentStats = new ArrayList<>();
							totalData.put(key, oldDocumentStats);
						}

						if (oldDocumentStats.size() == i) {
							/** 깊은 복사 */
							DocumentsStat doc = new DocumentsStat();
							doc.setKey(key);
							doc.setCount(documentsStat.getCount());
							doc.setSum(documentsStat.getSum());
							doc.setPow2sum(documentsStat.getPow2sum());
							doc.setMax(documentsStat.getMax());
							doc.setMin(documentsStat.getMin());

							doc.makeAvg();
							doc.makeStdev();

							oldDocumentStats.add(doc);
						} else {
							DocumentsStat oldDocumentStat = oldDocumentStats.get(i);

							oldDocumentStat.addCount(documentsStat.getCount());
							oldDocumentStat.addSum(documentsStat.getSum());
							oldDocumentStat.addPow2sum(documentsStat.getPow2sum());
							oldDocumentStat.makeAvg();
							oldDocumentStat.makeStdev();

							if (documentsStat.getMax() > oldDocumentStat.getMax()) {
								oldDocumentStat.setMax(documentsStat.getMax());
							}

							if (documentsStat.getMin() < oldDocumentStat.getMin()) {
								oldDocumentStat.setMin(documentsStat.getMin());
							}
						}
					}
				});
			}
		});

		List<String> list = totalData.entrySet().stream().sorted((entry1, entry2) -> {
			return DocumentStatUtils.compare(entry1.getValue(), entry2.getValue(), groupBySort);
		}).map(entry -> DocumentStatUtils.makeStringFromDocuments(entry.getValue())).collect(Collectors.toList());

		return list;
	}

	@Override
	public long getTotalCount() {
		return count;
	}

	@Override
	public boolean isFull() {
		return full;
	}

	@Override
	public Map<String, Map<String, List<DocumentsStat>>> getHistogramMap() {
		if (resultHistogramMap == null) {
			return new HashMap<>();
		}

		return resultHistogramMap.entrySet().stream().sorted(Map.Entry.<String, Map<String, List<DocumentsStat>>>comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
	}

	@Override
	public List<Double> getTotalValues() {
		return new ArrayList<>();
	}
}
