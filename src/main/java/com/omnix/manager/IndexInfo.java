package com.omnix.manager;

import java.time.LocalDateTime;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;

public class IndexInfo {
	/** table **/
	private long tableId;
	/** yyyymmddHH */
	private String indexKey;
	private IndexWriter indexWriter;
	private SearcherManager searcherManager;
	private ControlledRealTimeReopenThread<IndexSearcher> controlledRealTimeReopenThread;

	/** 마지막 참조 시간 */
	private volatile LocalDateTime lastReferenceData = LocalDateTime.now();

	public IndexInfo(long tableId, String indexKey, IndexWriter indexWriter, SearcherManager searcherManager, ControlledRealTimeReopenThread<IndexSearcher> controlledRealTimeReopenThread) {
		this.tableId = tableId;
		this.indexKey = indexKey;
		this.indexWriter = indexWriter;
		this.searcherManager = searcherManager;
		this.controlledRealTimeReopenThread = controlledRealTimeReopenThread;
	}

	public long getTableId() {
		return tableId;
	}

	public void setTableId(long tableId) {
		this.tableId = tableId;
	}

	public String getIndexKey() {
		return indexKey;
	}

	public void setIndexKey(String indexKey) {
		this.indexKey = indexKey;
	}

	public IndexWriter getIndexWriter() {
		return indexWriter;
	}

	public void setIndexWriter(IndexWriter indexWriter) {
		this.indexWriter = indexWriter;
	}

	public ControlledRealTimeReopenThread<IndexSearcher> getControlledRealTimeReopenThread() {
		return controlledRealTimeReopenThread;
	}

	public void setControlledRealTimeReopenThread(ControlledRealTimeReopenThread<IndexSearcher> controlledRealTimeReopenThread) {
		this.controlledRealTimeReopenThread = controlledRealTimeReopenThread;
	}

	public LocalDateTime getLastReferenceData() {
		return lastReferenceData;
	}

	public void taggingTime() {
		this.lastReferenceData = LocalDateTime.now();
	}

	public SearcherManager getSearcherManager() {
		return searcherManager;
	}

	public void setSearcherManager(SearcherManager searcherManager) {
		this.searcherManager = searcherManager;
	}

}
