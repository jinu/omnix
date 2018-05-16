package com.omnix.manager.statistics;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.util.Precision;

public class DocumentStatUtils {
	public static final String DIVIDE_FIELD = ",";
	public static final String LINE_FIELD = "|";

	public static List<DocumentsStat> makeDocumentsStats(String csvText) {
		List<DocumentsStat> result = new ArrayList<>();

		String[] textLine = StringUtils.splitPreserveAllTokens(csvText, LINE_FIELD);
		for (String text : textLine) {
			result.add(makeDocumentStat(text));
		}

		return result;
	}

	public static DocumentsStat makeDocumentStat(String csvText) {
		String[] field = StringUtils.splitPreserveAllTokens(csvText, DIVIDE_FIELD);

		DocumentsStat documentsStat = new DocumentsStat();
		documentsStat.setKey(field[0]);
		documentsStat.setCount(NumberUtils.toLong(field[1], 0L));
		documentsStat.setMin(NumberUtils.toDouble(field[2], 0L));
		documentsStat.setMax(NumberUtils.toDouble(field[3], 0L));
		documentsStat.setSum(NumberUtils.toDouble(field[4], 0L));

		documentsStat.setPow2sum(NumberUtils.toDouble(field[5], 0));
		documentsStat.setAvg(NumberUtils.toDouble(field[6], 0));
		documentsStat.setStdev(NumberUtils.toDouble(field[7], 0));

		return documentsStat;
	}

	public static String makeStringFromDocument(DocumentsStat documentsStat) {
		StringBuilder builder = new StringBuilder();

		builder.append(documentsStat.getKey());
		builder.append(DIVIDE_FIELD).append(documentsStat.getCount());

		if (documentsStat.getMin() == Long.MAX_VALUE) {
			builder.append(DIVIDE_FIELD).append(0L);
		} else {
			builder.append(DIVIDE_FIELD).append(Precision.round(documentsStat.getMin(), 3));
		}

		if (documentsStat.getMax() == Long.MIN_VALUE) {
			builder.append(DIVIDE_FIELD).append(0L);
		} else {
			builder.append(DIVIDE_FIELD).append(Precision.round(documentsStat.getMax(), 3));
		}

		builder.append(DIVIDE_FIELD).append(Precision.round(documentsStat.getSum(), 3));
		builder.append(DIVIDE_FIELD).append(Precision.round(documentsStat.getPow2sum(), 3));
		builder.append(DIVIDE_FIELD).append(Precision.round(documentsStat.getAvg(), 3));
		builder.append(DIVIDE_FIELD).append(Precision.round(documentsStat.getStdev(), 3));

		return builder.toString();
	}

	public static String makeStringFromDocuments(List<DocumentsStat> documentsStats) {
		List<String> texts = new ArrayList<>();
		for (DocumentsStat documentsStat : documentsStats) {
			texts.add(makeStringFromDocument(documentsStat));
		}

		return StringUtils.join(texts, LINE_FIELD);
	}

	public static int compare(String text1, String text2, GroupBySort groupBySort) {
		List<DocumentsStat> stats1 = makeDocumentsStats(text1);
		List<DocumentsStat> stats2 = makeDocumentsStats(text2);

		return compare(stats1, stats2, groupBySort);
	}

	public static int compare(List<DocumentsStat> stats1, List<DocumentsStat> stats2, GroupBySort groupBySort) {

		int compare = 0;

		switch (groupBySort) {
		case AVG:
			double avg1 = stats1.stream().mapToDouble(doc -> doc.getAvg()).sum();
			double avg2 = stats2.stream().mapToDouble(doc -> doc.getAvg()).sum();

			compare = Double.compare(avg2, avg1);
			break;

		case MIN:
			compare = Double.compare(stats2.get(0).getMin(), stats1.get(0).getMin());
			break;

		case MAX:
			compare = Double.compare(stats2.get(0).getMax(), stats1.get(0).getMax());
			break;

		case SUM:
			double sum1 = stats1.stream().mapToDouble(doc -> doc.getSum()).sum();
			double sum2 = stats2.stream().mapToDouble(doc -> doc.getSum()).sum();

			compare = Double.compare(sum2, sum1);
			break;

		case COUNT:
		default:
			long count1 = stats1.stream().mapToLong(doc -> doc.getCount()).sum();
			long count2 = stats2.stream().mapToLong(doc -> doc.getCount()).sum();

			compare = Long.compare(count2, count1);

			break;
		}
		return compare;
	}
}
