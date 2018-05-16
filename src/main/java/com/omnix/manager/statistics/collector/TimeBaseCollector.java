package com.omnix.manager.statistics.collector;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;

import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.statistics.DocumentsStat;

public class TimeBaseCollector extends AggregateCollector {
	private NumericDocValues timeDocValues; // timestamp
	private NumericDocValues[] numericDocValues;

	/** slot 개수 */
	private final int fieldCount;
	private final List<ColumnInfo> columnInfos;

	private long count = 0;

	private List<DocumentsStat> result = new ArrayList<>();
	private Map<Long, List<DocumentsStat>> histogramResult = new HashMap<>();

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	public TimeBaseCollector(List<ColumnInfo> columnInfos, long limit) {
		this.fieldCount = columnInfos.size();
		this.columnInfos = columnInfos;

		for (int i = 0; i < columnInfos.size(); i++) {
			result.add(new DocumentsStat());
		}
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.timeDocValues = DocValues.getNumeric(context.reader(), "_date2");
		this.numericDocValues = new NumericDocValues[fieldCount];

		for (int i = 0; i < fieldCount; i++) {
			numericDocValues[i] = DocValues.getNumeric(context.reader(), columnInfos.get(i).getName());
		}
	}

	@Override
	public void collect(int docID) throws IOException {
		count++;

		long timestamp = getValueLong(timeDocValues, docID);

		List<DocumentsStat> stats = histogramResult.get(timestamp);

		/** result에 없는 경우 put 해준다. */
		if (stats == null) {
			stats = new ArrayList<>();

			for (int i = 0; i < fieldCount; i++) {
				stats.add(new DocumentsStat());
			}

			histogramResult.put(timestamp, stats);
		}

		for (int i = 0; i < fieldCount; i++) {
			double val = getValueDouble(numericDocValues[i], docID, columnInfos.get(i).getLogFieldType());
			DocumentsStat documentsStat = stats.get(i);

			documentsStat.addCount(1L);
			documentsStat.addSum(val);
			documentsStat.addPow2sum(Math.pow(val, 2));

			if (val > documentsStat.getMax()) {
				documentsStat.setMax(val);
			}

			if (val < documentsStat.getMin()) {
				documentsStat.setMin(val);
			}

			DocumentsStat summaryStat = result.get(i);
			summaryStat.addCount(1L);
			summaryStat.addSum(val);
			summaryStat.addPow2sum(Math.pow(val, 2));

			if (val > summaryStat.getMax()) {
				summaryStat.setMax(val);
			}

			if (val < summaryStat.getMin()) {
				summaryStat.setMin(val);
			}
		}
	}

	private long getValueLong(NumericDocValues numericDocValues, int docID) throws IOException {
		boolean flag = numericDocValues.advanceExact(docID);

		long val = 0L;
		if (flag) {
			val = numericDocValues.longValue();
		}

		return val;
	}

	private double getValueDouble(NumericDocValues numericDocValues, int docID, LogFieldType logFieldType) throws IOException {
		boolean flag = numericDocValues.advanceExact(docID);

		double val = 0.0;
		if (flag) {
			if (logFieldType == LogFieldType.DOUBLE) {
				val = Double.longBitsToDouble(numericDocValues.longValue());
			} else {
				val = numericDocValues.longValue();
			}
		}

		return val;
	}

	@Override
	public boolean needsScores() {
		return false;
	}

	@Override
	public void merge() throws IOException {
	}

	@Override
	public List<String> getValues(int count) {
		List<String> temp = new ArrayList<>();

		for (DocumentsStat documentsStat : result) {
			documentsStat.makeAvg();
			documentsStat.makeStdev();

			temp.add(documentsStat.toCsvString());
		}

		return Arrays.asList(StringUtils.join(temp, "|"));
	}

	@Override
	public boolean isFull() {
		return false;
	}

	@Override
	public long getTotalCount() {
		return count;
	}

	@Override
	public void startHistogramCollect(boolean flag) {
	}

	@Override
	public void endHistogramCollect(String indexKey, LocalDateTime start, LocalDateTime finish) {
	}

	@Override
	public Map<String, Map<String, List<DocumentsStat>>> getHistogramMap() {
		Map<String, Map<String, List<DocumentsStat>>> returnData = new LinkedHashMap<>();

		histogramResult.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			ZonedDateTime currentTime = Instant.ofEpochSecond(entry.getKey()).atZone(ZoneId.systemDefault());
			String date = currentTime.format(DATE_FORMAT);

			entry.getValue().forEach(documentStat -> {
				documentStat.makeAvg();
				documentStat.makeStdev();

				documentStat.setKey("");
			});

			Map<String, List<DocumentsStat>> temp = new HashMap<>();
			temp.put("", entry.getValue());

			returnData.put(date, temp);
		});

		return returnData;
	}

	@Override
	public List<Double> getTotalValues() {
		return new ArrayList<>();
	}
}
