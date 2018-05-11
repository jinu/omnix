package com.omnix.manager.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 검색 결과 반환 bean
 */
public class SearchResult {
	private long jobId;
	/** 시작일 */
	private String date;
	/** 결과 개수 */
	private long count;
	/** 요청 개수 */
	private long limit;
	/** 정렬 */
	private boolean reverse;
	/** 쿼리 검색 시간 */
	private String queryTime;
	/** 결과 목록 */
	private List<SearchResultBean> lists = new ArrayList<>();

	/** 검색결과 담아져 있음 */
	private Map<String, Integer> histogram;

	/** 검색에 사용한 컬럼목록 */
	private Set<String> conditionColumn;

	/** 검색 진행 여부 */
	private boolean finish;

	public SearchResult(long jobId) {
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

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	public List<SearchResultBean> getLists() {
		return lists;
	}

	public void setLists(List<SearchResultBean> lists) {
		this.lists = lists;
	}

	public Map<String, Integer> getHistogram() {
		return histogram;
	}

	public void setHistogram(Map<String, Integer> result) {
		this.histogram = result;
	}

	public String getQueryTime() {
		return queryTime;
	}

	public void setQueryTime(String queryTime) {
		this.queryTime = queryTime;
	}

	public Set<String> getConditionColumn() {
		return conditionColumn;
	}

	public void setConditionColumn(Set<String> conditionColumn) {
		this.conditionColumn = conditionColumn;
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}
}
