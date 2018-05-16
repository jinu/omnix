package com.omnix.manager.statistics.collector;

import java.io.IOException;
import java.time.LocalDateTime;
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

public class GroupByTrendCollector extends AggregateCollector {
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

	/** slot */
	private List<DocumentsStat[]> documentsStatsSlots = new ArrayList<>();

	/** slot for count */
	private int[] countSlot;

	/** <날짜, <key, DocumentsStat[]>> */
	private Map<String, Map<String, List<DocumentsStat>>> histogramMap = new LinkedHashMap<>();
	/** <key, DocumentsStat[]>> */
	private Map<String, List<DocumentsStat>> groupMap = new HashMap<>();

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
	public GroupByTrendCollector(List<ColumnInfo> columnInfos, long limit, GroupBySort groupBySort) {
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
		} catch (IllegalStateException e) {
			/** statistics indexing 변경이 일어난 경우, 해당 context는 건너 뛴다. */
			keyIndex = null;
			return;
		}

		/** unique 한 key들의 count 값을 가져온다. */
		int keyCount = keyIndex.getValueCount();

		if (countOnly) {
			countSlot = new int[keyCount];
		} else {
			for (int i = 0; i < columnInfos.size(); i++) {
				ColumnInfo columnInfo = columnInfos.get(i);

				NumericDocValues numericIndex = null;
				DocumentsStat[] documentsStatsSlot = null;
				try {
					numericIndex = DocValues.getNumeric(context.reader(), columnInfo.getName());
					documentsStatsSlot = new DocumentsStat[keyCount];

					for (int j = 0; j < keyCount; j++) {
						documentsStatsSlot[j] = new DocumentsStat(j);
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
		if (keyIndex == null) {
			return;
		}

		boolean flag = keyIndex.advanceExact(doc);
		if (!flag) {
			return;
		}

		int ord = keyIndex.ordValue();
		countSlot[ord]++;
	}

	public void doAccumulate(int doc, ColumnInfo columnInfo, NumericDocValues numericIndex, DocumentsStat[] documentsStatsSlot) throws IOException {
		if (keyIndex == null || numericIndexes.size() == 0) {
			return;
		}

		boolean keyFlag = keyIndex.advanceExact(doc);
		if (!keyFlag) {
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

		int ord = keyIndex.ordValue();
		DocumentsStat documentsStat = documentsStatsSlot[ord];

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
			if (null == countSlot) {
				return;
			}

			for (int i = 0; i < countSlot.length; i++) {
				if (countSlot[i] > 0) {
					String key = keyIndex.lookupOrd(i).utf8ToString();
					List<DocumentsStat> lists = groupMap.get(key);

					/** 해당 group key에 count를 한다. */
					if (null == lists) {
						DocumentsStat documentsStat = new DocumentsStat();
						documentsStat.setCount(countSlot[i]);
						documentsStat.setKey(key);

						groupMap.put(key, Arrays.asList(documentsStat));
					} else {
						lists.get(0).addCount(countSlot[i]);
					}
				}
			}
		} else {
			if (CollectionUtils.isEmpty(documentsStatsSlots)) {
				return;
			}

			for (int i = 0; i < columnInfos.size(); i++) {
				for (DocumentsStat documentsStat : documentsStatsSlots.get(i)) {
					if (documentsStat.isFlag()) {
						int slot = documentsStat.getSlot();
						String key = keyIndex.lookupOrd(slot).utf8ToString();

						List<DocumentsStat> oldDocumentStats = groupMap.get(key);

						if (null == oldDocumentStats) {
							oldDocumentStats = new ArrayList<>();
							groupMap.put(key, oldDocumentStats);
						}

						/** slot 에 맞게 merge 처리 */
						if (oldDocumentStats.size() == i) {
							documentsStat.setKey(key);

							documentsStat.makeAvg();
							documentsStat.makeStdev();

							oldDocumentStats.add(documentsStat);

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
				}
			}

		}
		numericIndexes.clear();
		documentsStatsSlots.clear();

		countSlot = null;
		keyIndex = null;
	}

	@Override
	public void startHistogramCollect(boolean flag) {
		histogramCollectFlag = flag;
		/** data 초기화 */
		groupMap = new HashMap<>();
	}

	@Override
	public void endHistogramCollect(String indexKey, LocalDateTime start, LocalDateTime finish) {
		if (histogramCollectFlag) {
			histogramMap.put(indexKey, groupMap);
		}
		/** data 초기화 */
		groupMap = null;
	}

	/** group key 별 총합을 종합하여 준다. */
	@Override
	public List<String> getValues(int count) {
		Map<String, List<DocumentsStat>> totalData = new HashMap<>(); // key별 Map list

		histogramMap.values().stream().forEach(dataMap -> {
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
						for (DocumentsStat documentsStat : documentList) {

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
		if (histogramMap == null) {
			return new HashMap<>();
		}
		return histogramMap;
	}

	@Override
	public List<Double> getTotalValues() {
		return new ArrayList<>();
	}
}
