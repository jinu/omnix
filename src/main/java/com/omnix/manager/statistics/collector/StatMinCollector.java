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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.statistics.DocumentsStat;

public class StatMinCollector extends AggregateCollector {
	private final List<ColumnInfo> columnInfos;

	private long count = 0;
	private int[] timeCount;

	private long limit;
	private boolean full;
	private boolean countOnly;

	/** SortedDocValues */
	private NumericDocValues[] numericIndexes;
	private DocumentsStat[] documentsStats;

	private SortedDocValues timeStatDocValues; // timestamp
	private int[] timeSlot;

	/** 날짜, histoData 리스트 */
	private Map<String, DocumentsStat[]> histogramMap = new LinkedHashMap<>();

	private final List<DocumentsStat[]> histogramData = new ArrayList<>();

	/** Logger */
	private final Logger logger = LoggerFactory.getLogger(getClass());
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

	public StatMinCollector(List<ColumnInfo> columnInfos, long limit) {
		this.columnInfos = columnInfos;
		this.limit = limit;

		/** columnInfo 가 null 인 경우 count만 collect한다. */
		if (CollectionUtils.isEmpty(columnInfos)) {
			countOnly = true;
			documentsStats = new DocumentsStat[] { new DocumentsStat() };
		} else {
			documentsStats = new DocumentsStat[columnInfos.size()];
			numericIndexes = new NumericDocValues[columnInfos.size()];

			for (int i = 0; i < columnInfos.size(); i++) {
				documentsStats[i] = new DocumentsStat();
			}
		}

	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.timeStatDocValues = DocValues.getSorted(context.reader(), "_date2");
		int timeStatCount = timeStatDocValues.getValueCount();

		timeSlot = new int[timeStatCount];
		for (int i = 0; i <= timeStatCount - 1; i++) {
			timeSlot[i] = -1;
		}

		/** index 가 변경될때 처리함. */
		for (int i = 0; i < columnInfos.size(); i++) {
			try {
				numericIndexes[i] = DocValues.getNumeric(context.reader(), columnInfos.get(i).getName());
			} catch (Exception e) {
				numericIndexes[i] = null;
			}
		}

		logger.debug("doSetNextReader");
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

		boolean flag = timeStatDocValues.advanceExact(doc);
		if (!flag) {
			return;
		}

		timeStatDocValues.advanceExact(doc);
		int ord = timeStatDocValues.ordValue();
		int timeKey = timeSlot[ord];
		if (timeKey < 0) {
			String min = timeStatDocValues.lookupOrd(ord).utf8ToString();
			timeKey = NumberUtils.toInt(min);
			timeSlot[ord] = timeKey;
		}

		++count;
		++timeCount[timeKey];

		for (int i = 0; i < columnInfos.size(); i++) {
			doAccumulate(doc, numericIndexes[i], documentsStats[i], histogramData.get(timeKey)[i], columnInfos.get(i));
		}

	}

	public void doAccumulate(int doc, NumericDocValues numericIndex, DocumentsStat documentsStat, DocumentsStat histogramData, ColumnInfo columnInfo) throws IOException {
		if (null == numericIndex) {
			return;
		}

		boolean flag = numericIndex.advanceExact(doc);

		double val = 0L;
		if (flag) {

			if (columnInfo.getLogFieldType() == LogFieldType.DOUBLE) {
				val = Double.longBitsToDouble(numericIndex.longValue());
			} else {
				val = numericIndex.longValue();
			}
		}
		documentsStat.addCount(1L);
		documentsStat.addSum(val);
		documentsStat.addPow2sum(Math.pow(val, 2));

		if (val > documentsStat.getMax()) {
			documentsStat.setMax(val);
		}

		if (val < documentsStat.getMin()) {
			documentsStat.setMin(val);
		}

		if (histogramCollectFlag) {
			histogramData.addCount(1L);
			histogramData.addSum(val);
			histogramData.addPow2sum(Math.pow(val, 2));

			if (val > histogramData.getMax()) {
				histogramData.setMax(val);
			}

			if (val < histogramData.getMin()) {
				histogramData.setMin(val);
			}
		}
	}

	@Override
	public void merge() {
	}

	@Override
	public List<String> getValues(int count) {
		List<String> temp = new ArrayList<>();

		for (DocumentsStat documentsStat : documentsStats) {
			if (countOnly) {
				documentsStat.setCount(this.count);
			} else {
				documentsStat.makeAvg();
				documentsStat.makeStdev();
			}

			temp.add(documentsStat.toCsvString());
		}

		return Arrays.asList(StringUtils.join(temp, "|"));
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
	public void startHistogramCollect(boolean flag) {
		histogramCollectFlag = flag;

		histogramData.clear();

		timeCount = new int[60];
		for (int i = 0; i < 60; i++) {
			if (!countOnly) {
				DocumentsStat[] documentsStats = new DocumentsStat[columnInfos.size()];

				for (int j = 0; j < columnInfos.size(); j++) {
					documentsStats[j] = new DocumentsStat();
				}

				histogramData.add(documentsStats);
			}
		}
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

				Long rangeStart = Long.parseLong(formatter.format(start));
				Long rangeFinish = Long.parseLong(formatter.format(finish.minusNanos(1000L)));

				Long timeNumber = Long.parseLong(time.toString());

				if (rangeStart <= timeNumber && rangeFinish >= timeNumber) {

					if (countOnly) {
						DocumentsStat documentsStat = new DocumentsStat();
						documentsStat.setCount(timeCount[i]);
						documentsStat.setMin(0);
						documentsStat.setMax(0);

						DocumentsStat[] documentsStats = { documentsStat };

						histogramMap.put(time.toString(), documentsStats);
					} else {
						histogramMap.put(time.toString(), histogramData.get(i));
					}

				}

			}
		}
	}

	@Override
	public Map<String, Map<String, List<DocumentsStat>>> getHistogramMap() {
		if (histogramMap == null) {
			return new HashMap<>();
		}

		Map<String, Map<String, List<DocumentsStat>>> returnData = new LinkedHashMap<>();

		histogramMap.entrySet().stream().forEach(data -> {

			for (DocumentsStat documentsStat : data.getValue()) {
				if (!countOnly) {
					documentsStat.makeAvg();
					documentsStat.makeStdev();
				}
			}

			Map<String, List<DocumentsStat>> temp = new HashMap<>();
			temp.put("", Arrays.asList(data.getValue()));

			returnData.put(data.getKey(), temp);
		});

		return returnData;
	}

	@Override
	public List<Double> getTotalValues() {
		return new ArrayList<>();
	}
}
