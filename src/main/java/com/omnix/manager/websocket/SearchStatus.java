package com.omnix.manager.websocket;

import org.apache.commons.math3.util.Precision;

public class SearchStatus {
	private long jobId;
	private long count;
	private double percent;
	private String type;
	private boolean export;

	public SearchStatus() {
	}

	public SearchStatus(long jobId, long count, double percent, boolean export, String type) {
		this.jobId = jobId;
		this.count = count;
		this.percent = Precision.round(percent, 1);
		this.export = export;
		this.type = type;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public double getPercent() {
		return percent;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isExport() {
		return export;
	}

	public void setExport(boolean export) {
		this.export = export;
	}

}
