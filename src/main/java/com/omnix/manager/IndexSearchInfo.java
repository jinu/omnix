package com.omnix.manager;

import org.apache.lucene.search.IndexSearcher;

/**
 * indexsearcher 를 열때 사용하는 구조체 
 */
public class IndexSearchInfo {
	private boolean cache;
	private String indexKey;
	private IndexSearcher indexSearcher;
	
	public IndexSearchInfo(String indexKey) {
		this.indexKey = indexKey;
	}

	public boolean isCache() {
		return cache;
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}
	
	public String getIndexKey() {
		return indexKey;
	}

	public void setIndexKey(String indexKey) {
		this.indexKey = indexKey;
	}

	public IndexSearcher getIndexSearcher() {
		return indexSearcher;
	}

	public void setIndexSearcher(IndexSearcher indexSearcher) {
		this.indexSearcher = indexSearcher;
	}

}
