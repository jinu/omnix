package com.omnix.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.codecs.lucene50.Lucene50StoredFieldsFormat.Mode;
import org.apache.lucene.codecs.lucene70.Lucene70Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.omnix.config.Config;
import com.omnix.manager.parser.TableSchema;

/**
 * 모든 Index를 관리한다.
 */
@Component
public class IndexManager {
	@Autowired
	private Config config;

	private String indexDirectory;
	/** index writer cache {tableId_dateKey : value} */
	private LoadingCache<String, IndexInfo> indexCache;
	/** index close 시 처리하는 lock */
	private final ConcurrentHashMap<String, CountDownLatch> indexCloseLock = new ConcurrentHashMap<>();

	public static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");
	public static final double RAM_BUFFER_SIZE = 16.0;

	private static final AtomicBoolean CLOSE_FLAG = new AtomicBoolean(false);

	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public void init() {
		this.indexDirectory = config.getPath() + File.separator + "index";
		Path indexDirectoryPath = Paths.get(indexDirectory);
		if (!Files.isDirectory(indexDirectoryPath)) {
			try {
				Files.createDirectory(indexDirectoryPath);
			} catch (IOException e) {
				logger.error("critical error : ", e);
			}
		}

		/** index cahce 처리 */
		LoadingCache<String, IndexInfo> cache = CacheBuilder.newBuilder().maximumSize(config.getWriterCacheSize()).removalListener(new RemovalListener<String, IndexInfo>() {
			@Override
			public void onRemoval(RemovalNotification<String, IndexInfo> entry) {
				/** lock tagging 처리 */
				indexCloseLock.put(entry.getKey(), new CountDownLatch(1));

				try {
					logger.info("remove indexCache start : {}", entry.getKey());
					IndexInfo indexInfo = entry.getValue();

					indexInfo.getControlledRealTimeReopenThread().interrupt();
					indexInfo.getControlledRealTimeReopenThread().close();

					/** 해당 writer close */
					indexInfo.getIndexWriter().deleteUnusedFiles();
					indexInfo.getIndexWriter().close();

					indexInfo.getSearcherManager().close();

				} catch (Exception e) {
					logger.error("indexCache remove error", e);

				} finally {
					indexCloseLock.get(entry.getKey()).countDown();
					indexCloseLock.remove(entry.getKey());

					logger.info("remove indexCache finish : {}", entry.getKey());
				}
			}
		}).build(CacheLoader.<String, IndexInfo>from(indexKey -> {
			logger.info("create indexCache start : {}", indexKey);

			String[] temp = getTableNameAndKeyFromIndexKey(indexKey);
			long tableId = NumberUtils.toLong(temp[0]);
			String dateKey = temp[1];

			Path path = getPathByDateKey(tableId, dateKey);

			try {
				/** 해당 폴더에 대해, indexWriter를 생성한다. */
				Directory directory = NIOFSDirectory.open(path);
				IndexWriter indexWriter = new IndexWriter(directory, getIndexWriterConfig());
				indexWriter.commit();

				SearcherManager searcherManager = new SearcherManager(indexWriter, null);
				ControlledRealTimeReopenThread<IndexSearcher> controlledRealTimeReopenThread = new ControlledRealTimeReopenThread<>(indexWriter, searcherManager, config.getCommitSec(), config.getCommitSec());
				controlledRealTimeReopenThread.start();

				IndexInfo indexInfo = new IndexInfo(tableId, dateKey, indexWriter, searcherManager, controlledRealTimeReopenThread);
				return indexInfo;

			} catch (IOException e) {
				logger.error("index writer create error!", e);

			} finally {
				logger.info("create indexCache finish : {}", indexKey);
			}

			return null;
		}));

		if (indexCache != null) {
			indexCache.cleanUp();
			indexCache = null;
		}

		indexCache = cache;
	}

	public void addIndex(long tableId, String dateKey, List<Document> documents) throws IOException {
		if (CLOSE_FLAG.get()) {
			return;
		}

		String indexKey = getIndexKeyFromTableNameAndKey(tableId, dateKey);

		/** lock 확인 */
		CountDownLatch latch = indexCloseLock.get(indexKey);
		if (latch != null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				logger.error("addIndex writer lock wait fail", e);
			}
		}

		/** add index */
		try {
			IndexInfo indexInfo = indexCache.get(indexKey);
			indexInfo.getIndexWriter().addDocuments(documents);

		} catch (ExecutionException e) {
			logger.error("index cache get fail", e);
		}

	}

	public void deleteIndex(long tableId, String dateKey, Query query) throws IOException {
		String indexKey = getIndexKeyFromTableNameAndKey(tableId, dateKey);

		/** lock 확인 */
		CountDownLatch latch = indexCloseLock.get(indexKey);
		if (latch != null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				logger.error("addIndex writer lock wait fail", e);
			}
		}

		/** delete index */
		try {
			IndexInfo indexInfo = indexCache.get(indexKey);
			indexInfo.getIndexWriter().deleteDocuments(query);

		} catch (ExecutionException e) {
			logger.error("index cache get fail", e);
		}
	}

	public Optional<IndexSearchInfo> acquireSearcher(long tableId, String dateKey) {
		try {
			String indexKey = getIndexKeyFromTableNameAndKey(tableId, dateKey);
			IndexSearchInfo indexSearchInfo = new IndexSearchInfo(indexKey);

			IndexInfo indexInfo = indexCache.getIfPresent(indexKey);
			if (null != indexInfo) {
				/** lock 파일 존재 안하는 경우만 */
				if (!indexCloseLock.containsKey(indexKey)) {
					logger.info("acquireSearcher from cache : {}, {}", tableId, dateKey);
					
					indexSearchInfo.setIndexSearcher(indexInfo.getSearcherManager().acquire());
					indexSearchInfo.setCache(true);
					return Optional.of(indexSearchInfo);
				}
			}

			Path path = getPathByDateKey(tableId, dateKey);
			
			if (Files.isDirectory(path)) {
				logger.info("acquireSearcher from disk : {}, {}", tableId, dateKey);
				
				Directory directory = NIOFSDirectory.open(path);
				IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(directory));
				indexSearchInfo.setIndexSearcher(indexSearcher);

				return Optional.of(indexSearchInfo);
			}
			
		} catch (IOException e) {
			logger.error("acquireSearcher error", e);
		}

		return Optional.empty();
	}

	public void releaseSearcher(IndexSearchInfo indexSearchInfo) {
		if (indexSearchInfo.isCache()) {
			logger.info("releaseSearcher from cache : {}", indexSearchInfo.getIndexKey());
			
			IndexInfo indexInfo = indexCache.getIfPresent(indexSearchInfo.getIndexKey());

			if (null != indexInfo) {
				try {
					indexInfo.getSearcherManager().release(indexSearchInfo.getIndexSearcher());
				} catch (IOException e) {
					logger.info("release error", e);
				}
			}
		} else {
			logger.info("releaseSearcher from disk : {}", indexSearchInfo.getIndexKey());
			
			try {
				indexSearchInfo.getIndexSearcher().getIndexReader().close();
			} catch (IOException e) {
				logger.error("release error", e);
			}
		}
	}

	/**
	 * indexcache 에 존재하는 key를 변환한다.
	 */
	private String[] getTableNameAndKeyFromIndexKey(String key) {
		return StringUtils.split(key, "_", 2);
	}

	/**
	 * indexcache 에 존재하는 key를 변환한다.
	 */
	private String getIndexKeyFromTableNameAndKey(long tableId, String dateKey) {
		return new StringBuilder().append(tableId).append("_").append(dateKey).toString();
	}

	/**
	 * file path 를 얻어온다.
	 */
	private Path getPathByDateKey(long tableId, String dateKey) {
		String folderMonth = StringUtils.substring(dateKey, 0, 6); // yyyyMM
		String folderDay = StringUtils.substring(dateKey, 6, 8); // dd
		String folderHour = StringUtils.substring(dateKey, 8, 10); // HH

		Path path = Paths.get(indexDirectory + File.separator + String.valueOf(tableId) + File.separator + folderMonth + File.separator + folderDay + File.separator + folderHour);
		return path;
	}

	public void deleteTable(TableSchema tableSchema) {
		List<String> keys = indexCache.asMap().entrySet().stream().filter(entry -> entry.getValue().getTableId() == tableSchema.getId()).map(t -> t.getKey()).collect(Collectors.toList());
		indexCache.invalidateAll(keys);

		try {
			FileUtils.deleteDirectory(new File(indexDirectory + File.separator + tableSchema.getId()));
		} catch (IOException e) {
			logger.error("delete directory fail", e);
		}
	}
	
	public void createTable(TableSchema tableSchema) throws IOException {
		FileUtils.forceMkdir(new File(indexDirectory + File.separator + tableSchema.getId()));
	}

	/**
	 * index writer 기본 설정
	 */
	private IndexWriterConfig getIndexWriterConfig() {
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new TsmAnalyzer());
		indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
		indexWriterConfig.setRAMBufferSizeMB(RAM_BUFFER_SIZE);
		indexWriterConfig.setMergePolicy(new LogByteSizeMergePolicy());

		indexWriterConfig.setCodec(new Lucene70Codec(Mode.BEST_COMPRESSION));
		indexWriterConfig.setUseCompoundFile(false);

		return indexWriterConfig;
	}

	@PreDestroy
	public void destroy() {
		CLOSE_FLAG.set(true);
		
		indexCache.invalidateAll();
	}
}
