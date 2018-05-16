package com.omnix.manager.statistics.collector;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;

import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.statistics.DocumentsStat;

public class TimeStatCollector extends AggregateCollector {

	/** NumericDocValues */

	private NumericDocValues valueIndex;

	/** SortedDocValues */
	private SortedDocValues keyIndex;
	private SortedDocValues timeStatDocValues;

	/** result List */
	private List<Map<String, DocumentsStat>> result = new ArrayList<>();
	private Map<String, Map<String, DocumentsStat>> resultData = new LinkedHashMap<>();

	/** keyIndex ord */
	private String[] keyOrdValueList;
	private List<ColumnInfo> columnInfos;

	/** key field */
	private final String keyField;
	/** value field */
	private String valueField;

	private long count;
	private boolean countOnly = false;
	private Map<Integer, int[]> tempMap = new HashMap<>();
	private int[] timeSlot;

	/**
	 * Time Stat 생성자
	 * 
	 * @param columnInfos
	 * @param limit
	 * @param startValue
	 *            : 시작 시간
	 * @param period
	 *            : 주기
	 */
	public TimeStatCollector(List<ColumnInfo> columnInfos, long limit) {
		this.keyField = columnInfos.get(0).getName();

		if (columnInfos.size() == 1 || null == columnInfos.get(1)) {
			countOnly = true;
		} else {
			this.valueField = columnInfos.get(1).getName();
		}

		this.columnInfos = columnInfos;

		/** 주기를 계산하여 result를 세팅한다. */
		for (int i = 0; i <= 60; i++) {
			result.add(new HashMap<>());
		}
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		merge();
		this.timeStatDocValues = DocValues.getSorted(context.reader(), "_date2");
		int timeStatCount = timeStatDocValues.getValueCount();

		timeSlot = new int[timeStatCount];
		for (int i = 0; i <= timeStatCount - 1; i++) {
			timeSlot[i] = -1;
		}

		keyIndex = DocValues.getSorted(context.reader(), keyField);
		keyOrdValueList = new String[keyIndex.getValueCount()];
		if (!countOnly) {
			valueIndex = DocValues.getNumeric(context.reader(), valueField);
			/** keyIndex ord setting */
		}

	}

	@Override
	public void collect(int doc) throws IOException {
		count++;

		boolean flag = timeStatDocValues.advanceExact(doc);
		if (!flag) {
			return;
		}

		int timeOrd = timeStatDocValues.ordValue();
		int timeKey = timeSlot[timeOrd];
		if (timeKey < 0) {
			String min = timeStatDocValues.lookupOrd(timeOrd).utf8ToString();
			timeKey = NumberUtils.toInt(min);
			timeSlot[timeOrd] = timeKey;
		}

		boolean keyFlag = keyIndex.advanceExact(doc);
		if (!keyFlag) {
			return;
		}

		int ord = keyIndex.ordValue();
		if (countOnly) {
			int[] valueSlot = tempMap.get(timeKey);
			if (valueSlot == null) {
				// TODO
				valueSlot = new int[keyIndex.getValueCount()];
			}

			valueSlot[ord]++;
			tempMap.put(timeKey, valueSlot);

		} else {
			/** keyIndex ord -> utf8ToString */
			String key = keyOrdValueList[ord];
			if (key == null) {
				key = keyIndex.lookupOrd(ord).utf8ToString();
				keyOrdValueList[ord] = key;
			}

			DocumentsStat documentsStat = result.get(timeKey).get(key);
			if (null == documentsStat) {
				documentsStat = new DocumentsStat();
				result.get(timeKey).put(key, documentsStat);
			}
			boolean numericFlag = valueIndex.advanceExact(doc);
			double val = 0L;
			if (numericFlag) {
				if (columnInfos.get(1).getLogFieldType() == LogFieldType.DOUBLE) {
					val = Double.longBitsToDouble(valueIndex.longValue());
				} else {
					val = valueIndex.longValue();
				}
			}
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
	}

	@Override
	public void merge() throws IOException {
		if (countOnly) {

			if (null == tempMap) {
				return;
			}

			tempMap.entrySet().forEach(entry -> {
				for (int i = 0; i < entry.getValue().length; i++) {
					try {
						if (entry.getValue()[i] <= 0) {
							continue;
						}

						String key = keyOrdValueList[i];
						if (key == null) {
							key = keyIndex.lookupOrd(i).utf8ToString();
							keyOrdValueList[i] = key;
						}

						DocumentsStat documentsStat = new DocumentsStat();
						documentsStat.setCount(entry.getValue()[i]);
						documentsStat.setMin(0);
						documentsStat.setMax(0);

						result.get(entry.getKey()).put(key, documentsStat);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}

		/** collector clear & null */
		tempMap.clear();
		keyOrdValueList = null;
		keyIndex = null;
	}

	@Override
	public Map<String, Map<String, List<DocumentsStat>>> getHistogramMap() {
		if (resultData == null) {
			return new HashMap<>();
		}

		Map<String, Map<String, List<DocumentsStat>>> returnData = new LinkedHashMap<>();

		resultData.entrySet().stream().forEach(data -> {
			Map<String, List<DocumentsStat>> temp = new HashMap<>();
			data.getValue().entrySet().stream().forEach(entry -> {

				DocumentsStat documentsStat = entry.getValue();
				if (!countOnly) {
					documentsStat.makeAvg();
					documentsStat.makeStdev();
				}

				temp.put(entry.getKey(), Arrays.asList(new DocumentsStat[] { documentsStat }));
			});
			returnData.put(data.getKey(), temp);
		});

		return returnData;
	}

	@Override
	public List<String> getValues(int count) {
		return new ArrayList<>();
	}

	@Override
	public long getTotalCount() {
		// TODO Auto-generated method stub
		return count;
	}

	@Override
	public boolean needsScores() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFull() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startHistogramCollect(boolean flag) {
		histogramCollectFlag = flag;
	}

	@Override
	public void endHistogramCollect(String indexKey, LocalDateTime start, LocalDateTime finish) {
		if (histogramCollectFlag) {
			for (int i = 0; i < 60; i++) {
				StringBuilder time = new StringBuilder();
				time.append(indexKey);

				if (i < 10) {
					time.append("0").append(i);
				} else {
					time.append(i);
				}

				resultData.put(time.toString(), result.get(i));

			}
		}

	}

	@Override
	public List<Double> getTotalValues() {
		return new ArrayList<>();
	}
}
