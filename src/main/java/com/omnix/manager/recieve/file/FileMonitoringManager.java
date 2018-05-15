package com.omnix.manager.recieve.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.omnix.manager.IndexManager;
import com.omnix.manager.LogManager;
import com.omnix.manager.parser.ColumnInfoManager;
import com.omnix.manager.recieve.LogBean;
import com.omnix.manager.repository.FileMonitoringInfoRepository;

@Service
public class FileMonitoringManager {
	@Autowired
	private FileMonitoringInfoRepository fileMonitoringInfoRepository;
	@Autowired
	private LogManager logManager;

	private volatile List<FileMonitoringInfo> lists;

	private final Logger statusLogger = LoggerFactory.getLogger("status");
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@PostConstruct
	public void init() {
		reloadAll();
	}

	public synchronized void reloadAll() {
		lists = fileMonitoringInfoRepository.findAll();
	}

	public List<FileMonitoringInfo> getFileMonitoringInfoListByScriptId(long tableId, long scriptId) {
		return fileMonitoringInfoRepository.findAllByScriptInfoIdAndScriptInfoTableSchemaId(tableId, scriptId, new Sort(Sort.Direction.ASC, "id"));
	}

	public List<FileMonitoringInfo> getFileMonitoringInfoListByTableId(long tableId) {
		return fileMonitoringInfoRepository.findAllByScriptInfoTableSchemaId(tableId, new Sort(Sort.Direction.ASC, "id"));
	}

	public List<FileMonitoringInfo> getFileMonitoringInfoList() {
		return fileMonitoringInfoRepository.findAll(new Sort(Sort.Direction.ASC, "id"));
	}

	public Optional<FileMonitoringInfo> getFileMonitoringInfo(long id) {
		return fileMonitoringInfoRepository.findById(id);
	}

	public FileMonitoringInfo getFileMonitoringInfoForUpdate(long id) {
		return fileMonitoringInfoRepository.getOne(id);
	}

	public FileMonitoringInfo saveFileMonitoringInfo(FileMonitoringInfo fileMonitoringInfo) {
		fileMonitoringInfo = fileMonitoringInfoRepository.saveAndFlush(fileMonitoringInfo);
		reloadAll();
		return fileMonitoringInfo;
	}

	public void deleteFileMonitoringInfo(FileMonitoringInfo fileMonitoringInfo) {
		fileMonitoringInfoRepository.delete(fileMonitoringInfo);
		reloadAll();
	}

	@Scheduled(fixedDelay = 1000, initialDelay = 1000)
	public void loadFile() {
		if (!ColumnInfoManager.INIT.get()) {
			return;
		}

		if (!IndexManager.INIT.get()) {
			return;
		}

		for (FileMonitoringInfo fileMonitoringInfo : lists) {
			if (!fileMonitoringInfo.isEnable()) {
				continue;
			}

			Path monitoringDirectory = Paths.get(fileMonitoringInfo.getPath());
			if (!Files.isDirectory(monitoringDirectory)) {
				continue;
			}

			/** 삭제 처리될 구조체 */
			List<Path> deleteFileList = new ArrayList<>();

			try (Stream<Path> files = Files.list(monitoringDirectory)) {
				List<Path> paths = files.filter(t -> t.toString().endsWith(".log")).sorted().collect(Collectors.toList());

				if (paths.size() > 0) {
					StopWatch stopWatch = new StopWatch();
					stopWatch.start();

					statusLogger.info("start loadFile count : {}", paths.size());
					for (Path path : paths) {
						int i = 0;
						try (FileInputStream stream1 = new FileInputStream(path.toFile()); InputStreamReader stream2 = new InputStreamReader(stream1, "UTF-8"); BufferedReader br = new BufferedReader(stream2)) {
							String line;
							while ((line = br.readLine()) != null) {
								readLog(line, fileMonitoringInfo);
								i++;
							}

							if (i % 2_0000 == 0) {
								logManager.awaitQueue();
							}
						} finally {
							deleteFileList.add(path);
						}

						statusLogger.info("load file : {}, count : {}", path, i);

					}

					stopWatch.stop();
					statusLogger.info("finish loadFile count : {}, elapsed : {}", paths.size(), stopWatch);
				}

			} catch (Exception e) {
				logger.error("file read error", e);
			}

			/** 처리 된 파일 삭제 */
			for (Path path : deleteFileList) {
				deleteFileWithCanonical(path.toFile());
			}

		}
	}

	private void readLog(String text, FileMonitoringInfo fileMonitoringInfo) {
		if (StringUtils.isEmpty(text)) {
			return;
		}

		LogBean logBean = new LogBean(fileMonitoringInfo.getScriptInfo().getTableSchema().getId(), fileMonitoringInfo.getScriptInfo().getId(), text, null);
		LogManager.sendLog(logBean);
	}

	private void deleteFileWithCanonical(File file) {
		try {
			File canonicalFile = file.getCanonicalFile();
			canonicalFile.delete();
		} catch (IOException e) {
			logger.error("Fail to delete canonical file : " + e.toString());
		} finally {
			file.delete();
		}
	}
}
