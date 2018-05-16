package com.omnix.manager.statistics.collector;

import java.io.IOException;
import java.time.LocalDateTime;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.statistics.DocumentsStat;

public class StatCollector extends AggregateCollector {
	private final List<ColumnInfo> columnInfos;

	private long count = 0;

	private long limit;
	private boolean full;
	private boolean countOnly;

	/** SortedDocValues */
	private NumericDocValues[] numericIndexes;

	private DocumentsStat[] documentsStats;

	/** 날짜, histoData 리스트 */
	private Map<String, DocumentsStat[]> histogramMap = new LinkedHashMap<>();

	private DocumentsStat[] histogramData;

	/** Logger */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public StatCollector(List<ColumnInfo> columnInfos, long limit) {
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
		if (countOnly) {
			return;
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

		++count;

		for (int i = 0; i < columnInfos.size(); i++) {
			doAccumulate(doc, numericIndexes[i], documentsStats[i], histogramData[i], columnInfos.get(i));
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
		histogramData = new DocumentsStat[columnInfos.size()];

		for (int i = 0; i < columnInfos.size(); i++) {
			histogramData[i] = new DocumentsStat();
		}
	}

	@Override
	public void endHistogramCollect(String indexKey, LocalDateTime start, LocalDateTime finish) {
		if (histogramCollectFlag) {
			histogramMap.put(indexKey, histogramData);
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
				if (countOnly) {
					documentsStat.setCount(this.count);
				} else {
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
