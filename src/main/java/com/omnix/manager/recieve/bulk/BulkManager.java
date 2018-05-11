package com.omnix.manager.recieve.bulk;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.omnix.manager.LogManager;
import com.omnix.manager.parser.ScriptInfo;
import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.recieve.LogBean;

/**
 * 로그 벌크처리 용도
 */
@Component
public class BulkManager {

	@Autowired
	private LogManager logManager;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final List<BulkTargetInfo> bulkTargetList = new ArrayList<>(); // rawlog 가 있는 파일 캐시
	private TableSchema tableSchema;
	private ScriptInfo scriptInfo;

	private final AtomicBoolean run = new AtomicBoolean(false);
	private final AtomicBoolean stop = new AtomicBoolean(false);

	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public boolean prepare(TableSchema tableSchema, ScriptInfo scriptInfo, String uri) throws IOException {
		if (run.get()) {
			return false;
		}

		this.tableSchema = tableSchema;
		this.scriptInfo = scriptInfo;

		boolean flag = readTarget(uri);
		return flag;
	}

	/**
	 * 타겟을 읽는다.
	 */
	public boolean readTarget(String uri) throws IOException {
		if (run.get()) {
			return false;
		}

		Path target = Paths.get(uri);
		if (!Files.isDirectory(target)) {
			return false;
		}

		bulkTargetList.clear();

		Files.walk(target).filter(t -> !Files.isDirectory(t)).filter(path -> StringUtils.endsWith(path.toString(), "gz")).map(path -> {
			BulkTargetInfo bulkTargetInfo = new BulkTargetInfo();
			bulkTargetInfo.setPath(path);
			return bulkTargetInfo;
		}).sorted((t1, t2) -> {
			return t1.getPath().getFileName().compareTo(t2.getPath().getFileName());
		}).forEach(t -> {
			bulkTargetList.add(t);
		});

		return true;
	}

	public boolean stop() {
		if (!run.get()) {
			return false;
		}

		stop.set(true);
		return false;
	}

	public boolean run() {
		if (run.get()) {
			return false;
		}

		/** TARGET_LIST 체크 */
		if (CollectionUtils.isEmpty(bulkTargetList)) {
			return false;
		}

		run.set(true);
		stop.set(false);

		executor.submit(() -> {
			bulkTargetList.stream().forEach(targetInfo -> {
				if (stop.get()) {
					return;
				}

				try {
					gzipRead(targetInfo);
				} catch (IOException e) {
					logger.error("gzip error", e);
				} finally {
					targetInfo.setFinish(LocalDateTime.now());
				}
			});

			run.set(false);
		});

		return true;
	}

	/**
	 * Gzip 을 읽는다.
	 */
	public void gzipRead(BulkTargetInfo targetInfo) throws IOException {
		logManager.awaitQueue();

		logger.info("gzipRead start : {}", targetInfo);
		targetInfo.setStart(LocalDateTime.now());
		
		long size = 0;
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(targetInfo.getPath().toFile())), "UTF-8"))) {
			String text;
			while ((text = buffer.readLine()) != null) {
				if (stop.get()) {
					break;
				}

				LogBean logBean = new LogBean(tableSchema.getId(), scriptInfo.getId(), text, null);
				LogManager.sendLog(logBean);

				size++;
				targetInfo.increment();

				/** 20000 라인당 한번씩 queue를 들여다본다. */
				if (size % 2_0000 == 0) {
					logManager.awaitQueue();
				}

			}
		}

		logger.info("gzipRead stop : {}", targetInfo);
	}

	public List<BulkTargetInfo> getBulkTargetList() {
		return bulkTargetList;
	}

	public TableSchema getTableSchema() {
		return tableSchema;
	}

	public ScriptInfo getScriptInfo() {
		return scriptInfo;
	}

	public AtomicBoolean getRun() {
		return run;
	}

}
