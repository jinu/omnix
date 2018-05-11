package com.omnix.manager.recieve.bulk;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class BulkTargetInfo {
	private Path path;
	private LocalDateTime start;
	private LocalDateTime finish;
	private long count = 0L;

	public BulkTargetInfo() {
	}

	public BulkTargetInfo(Path path) {
		this.path = path;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	public LocalDateTime getFinish() {
		return finish;
	}

	public void setFinish(LocalDateTime finish) {
		this.finish = finish;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}
	
	public void increment() {
		this.count++;
	}

	@Override
	public String toString() {
		return "TargetInfo [path=" + path + ", start=" + start + ", finish=" + finish + ", count=" + count + "]";
	}

}
