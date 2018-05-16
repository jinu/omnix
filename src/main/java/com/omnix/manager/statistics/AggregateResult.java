package com.omnix.manager.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AggregateResult {
	private long jobId;
	/** 시작일 */
	private String date;
	/** 쿼리 검색 시간 */
	private String queryTime;
	/** 결과 개수 */
	private long count;
	/** 결과 목록 */
	private List<String> result;
	/** column 값의 총합 */
	private List<Double> columnValueList = new ArrayList<>();
	/** 결과 목록 */
	private Map<String, Map<String, List<DocumentsStat>>> histogramResult;

	/** 검색 진행 여부 */
	private boolean finish;
	
	public AggregateResult() {
	}
	
	public AggregateResult(long jobId) {
		this.jobId = jobId;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getQueryTime() {
		return queryTime;
	}

	public void setQueryTime(String queryTime) {
		this.queryTime = queryTime;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public List<String> getResult() {
		return result;
	}

	public void setResult(List<String> result) {
		this.result = result;
	}

	public List<Double> getColumnValueList() {
		return columnValueList;
	}

	public void setColumnValueList(List<Double> columnValueList) {
		this.columnValueList = columnValueList;
	}

	public Map<String, Map<String, List<DocumentsStat>>> getHistogramResult() {
		return histogramResult;
	}

	public void setHistogramResult(Map<String, Map<String, List<DocumentsStat>>> histogramResult) {
		this.histogramResult = histogramResult;
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}

}
