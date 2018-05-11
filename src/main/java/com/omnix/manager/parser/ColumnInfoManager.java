package com.omnix.manager.parser;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.omnix.manager.LogManager;
import com.omnix.manager.repository.ColumnInfoRepository;

@Service
public class ColumnInfoManager {
	@Autowired
	private TableSchemaManager tableSchemaManager;
	@Autowired
	private ScriptInfoManager scriptInfoManager;
	@Autowired
	private ParserInfoManager parserInfoManager;
	@Autowired
	private ColumnInfoRepository columnInfoRepository;
	@Autowired
	private LogManager logManager;

	/** {tableId: {name : columnInfo}} */
	public static final Map<Long, Map<String, ColumnInfo>> COLUMNINFO_CACHE = new ConcurrentHashMap<>();
	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@PostConstruct
	public void init() {
		tableSchemaManager.init();

		TableSchemaManager.getListCache().forEach(tableSchema -> {
			COLUMNINFO_CACHE.put(tableSchema.getId(), new ConcurrentHashMap<>());
		});

		columnInfoRepository.findAll().forEach(t -> {
			COLUMNINFO_CACHE.get(t.getTableSchema().getId()).put(t.getName(), t);
		});

		scriptInfoManager.init();
		parserInfoManager.init();

		logManager.init();
	}

	public static ColumnInfo getColumnInfoCache(long tableId, String columnName) {
		Map<String, ColumnInfo> temp = COLUMNINFO_CACHE.get(tableId);
		if (null != temp) {
			return temp.get(columnName);
		}

		return null;
	}

	public static Map<String, ColumnInfo> getColumnInfoCache(long tableId) {
		return COLUMNINFO_CACHE.get(tableId);
	}

	public static List<ColumnInfo> getColumnInfoCacheList(TableSchema tableSchema) {
		Map<String, ColumnInfo> temp = COLUMNINFO_CACHE.get(tableSchema.getId());
		if (null == temp) {
			return Arrays.asList();
		}

		return temp.values().stream().sorted(Comparator.comparingLong(ColumnInfo::getId)).collect(Collectors.toList());
	}

	public List<ColumnInfo> getColumnInfoList(long tableId) {
		return columnInfoRepository.findAllByTableSchemaId(tableId, new Sort(Sort.Direction.ASC, "id"));
	}

	public Optional<ColumnInfo> getColumnInfo(long id) {
		return columnInfoRepository.findById(id);
	}

	public ColumnInfo getColumnInfoForUpdate(long id) {
		return columnInfoRepository.getOne(id);
	}

	public ColumnInfo saveColumnInfo(ColumnInfo columnInfo) {
		if (columnInfo.getId() == 0L) {
			columnInfoRepository.saveAndFlush(columnInfo);

		} else {
			ColumnInfo oldColumnInfo = columnInfoRepository.getOne(columnInfo.getId());

			oldColumnInfo.setAlias(columnInfo.getAlias());
			oldColumnInfo.setSearch(columnInfo.isSearch());
			oldColumnInfo.setStatistics(columnInfo.isStatistics());
			oldColumnInfo.setDescription(columnInfo.getDescription());	

			columnInfo = columnInfoRepository.saveAndFlush(oldColumnInfo);
		}

		COLUMNINFO_CACHE.get(columnInfo.getTableSchema().getId()).put(columnInfo.getName(), columnInfo);
		return columnInfo;
	}

	public void deleteColumnInfo(ColumnInfo columnInfo) {
		if (null == columnInfo) {
			return;
		}

		columnInfoRepository.delete(columnInfo);
		COLUMNINFO_CACHE.get(columnInfo.getTableSchema().getId()).remove(columnInfo.getName());
	}

}
