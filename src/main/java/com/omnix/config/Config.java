package com.omnix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 기본 설정
 */
@Component
@ConfigurationProperties(prefix = "config", ignoreInvalidFields = true)
public class Config {
	/** 경로 */
	private String path;
	/** 로그 경로 */
	private String logPath;
	/** commit 주기 */
	private double commitSec = 2.0;
	/** log bufer max ms */
	private long logBufferMs = 1L;
	/** log buffer max count */
	private int logBufferCount = 5_0000;
	/** log buffer limit */
	private int logBufferLimit = 20_0000;
	/** indexwriter thread */
	private int writerThread = 4;
	/** indexwriter cache size */
	private long writerCacheSize = 4L;
	/** search cache */
	private long searchCacheMaxHour = 1L;

	/** 파일 모니터링 경로 */
	private String fileMonitoringPath;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public double getCommitSec() {
		return commitSec;
	}

	public void setCommitSec(double commitSec) {
		this.commitSec = commitSec;
	}

	public long getLogBufferMs() {
		return logBufferMs;
	}

	public void setLogBufferMs(long logBufferMs) {
		this.logBufferMs = logBufferMs;
	}

	public int getLogBufferCount() {
		return logBufferCount;
	}

	public void setLogBufferCount(int logBufferCount) {
		this.logBufferCount = logBufferCount;
	}

	public int getLogBufferLimit() {
		return logBufferLimit;
	}

	public void setLogBufferLimit(int logBufferLimit) {
		this.logBufferLimit = logBufferLimit;
	}

	public int getWriterThread() {
		return writerThread;
	}

	public void setWriterThread(int writerThread) {
		this.writerThread = writerThread;
	}

	public long getWriterCacheSize() {
		return writerCacheSize;
	}

	public void setWriterCacheSize(long writerCacheSize) {
		this.writerCacheSize = writerCacheSize;
	}

	public long getSearchCacheMaxHour() {
		return searchCacheMaxHour;
	}

	public void setSearchCacheMaxHour(long searchCacheMaxHour) {
		this.searchCacheMaxHour = searchCacheMaxHour;
	}

	public String getFileMonitoringPath() {
		return fileMonitoringPath;
	}

	public void setFileMonitoringPath(String fileMonitoringPath) {
		this.fileMonitoringPath = fileMonitoringPath;
	}

}
