package com.omnix.manager;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;

public class TsmAnalyzer extends Analyzer {
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		return new TokenStreamComponents(new ICUTokenizer());
	}
}