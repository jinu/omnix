package com.omnix.manager.statistics;

import java.util.Arrays;

import com.omnix.manager.search.SearchType;

public class AggregateSupport {
	private long jobId;
	/** 검색 기준 */
	private SearchType searchType = SearchType.DAY;
	/** 날짜 범위 */
	private String dateRange;
	/** 검색 조건 */
	private String query;
	/** 통계 타입 */
	private AggregateType aggregateType;

	/** list order 기준 */
	private GroupBySort groupBySort = GroupBySort.COUNT;
	/** index 역순부터 */
	private boolean reverse = true;

	/** 최대 limit */
	private int limit = 1_0000_0000;
	/** top 몇개까지 */
	private int topCount = 100;
	/** 타겟 컬럼정보 */
	private String[] columns;
	/** histogram collect flag */
	private boolean histogramFlag = true;

	/** 시간 주기 */
	private long period;

	/** 시간 refresh */
	private boolean refresh = false;

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public SearchType getSearchType() {
		return searchType;
	}

	public void setSearchType(SearchType searchType) {
		this.searchType = searchType;
	}

	public String getDateRange() {
		return dateRange;
	}

	public void setDateRange(String dateRange) {
		this.dateRange = dateRange;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public AggregateType getAggregateType() {
		return aggregateType;
	}

	public void setAggregateType(AggregateType statisticsType) {
		this.aggregateType = statisticsType;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getTopCount() {
		return topCount;
	}

	public void setTopCount(int topCount) {
		this.topCount = topCount;
	}

	public String[] getColumns() {
		return columns;
	}

	public void setColumns(String[] columns) {
		this.columns = columns;
	}

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	public GroupBySort getGroupBySort() {
		return groupBySort;
	}

	public void setGroupBySort(GroupBySort groupBySort) {
		this.groupBySort = groupBySort;
	}

	public boolean isHistogramFlag() {
		return histogramFlag;
	}

	public void setHistogramFlag(boolean histogramFlag) {
		this.histogramFlag = histogramFlag;
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long period) {
		this.period = period;
	}

	public boolean isRefresh() {
		return refresh;
	}

	public void setRefresh(boolean refresh) {
		this.refresh = refresh;
	}

	@Override
	public String toString() {
		return "AggregateSupport [searchType=" + searchType + ", dateRange=" + dateRange + ", query=" + query + ", aggregateType=" + aggregateType + ", groupBySort=" + groupBySort + ", limit=" + limit + ", topCount=" + topCount + ", columns=" + Arrays.toString(columns) + "]";
	}
}
