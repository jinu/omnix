package com.omnix.manager.parser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.omnix.config.UiValidationException;
import com.omnix.manager.IndexManager;
import com.omnix.manager.repository.TableSchemaRepository;

@Service
public class TableSchemaManager {
	@Autowired
	private TableSchemaRepository tableSchemaRepository;
	@Autowired
	private IndexManager indexManager;

	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final Map<Long, TableSchema> TABLE_CACHE = new ConcurrentHashMap<>();

	public void init() {
		indexManager.init();

		List<TableSchema> lists = tableSchemaRepository.findAll();

		/** default 처리 */
		if (lists.isEmpty()) {
			TableSchema tableSchema = new TableSchema();
			tableSchema.setName("default");

			try {
				indexManager.createTable(tableSchema);
			} catch (IOException e) {
				logger.error("critical error. default schema create fail", e);
			}

			tableSchemaRepository.saveAndFlush(tableSchema);
			lists.add(tableSchema);
		}

		lists.forEach(t -> {
			TABLE_CACHE.put(t.getId(), t);
		});
	}

	public static TableSchema getTableSchemaCache(long id) {
		return TABLE_CACHE.get(id);
	}

	public Optional<TableSchema> getTableSchema(long id) {
		return tableSchemaRepository.findById(id);
	}

	public TableSchema getTableSchemaForUpdate(long id) {
		return tableSchemaRepository.getOne(id);
	}

	public TableSchema saveTableSchema(TableSchema tableSchema, boolean createFlag) {
		if (createFlag) {
			tableSchemaRepository.saveAndFlush(tableSchema);
		} else {
			TableSchema oldTableSchema = tableSchemaRepository.getOne(tableSchema.getId());

			oldTableSchema.setDescription(tableSchema.getDescription());
			tableSchema = tableSchemaRepository.saveAndFlush(oldTableSchema);
		}

		TABLE_CACHE.put(tableSchema.getId(), tableSchema);

		if (createFlag) {
			try {
				indexManager.createTable(tableSchema);

			} catch (IOException e) {
				TABLE_CACHE.remove(tableSchema.getId());
				tableSchemaRepository.delete(tableSchema);

				throw new UiValidationException("ALERT_TABLE_CREATE");
			}
		}

		return tableSchema;
	}

	public void deleteTableSchema(TableSchema tableSchema) {
		TABLE_CACHE.remove(tableSchema.getId());
		
		tableSchemaRepository.delete(tableSchema);
		indexManager.deleteTable(tableSchema);
		
		//TODO reload all
	}

	public static List<TableSchema> getListCache() {
		return TABLE_CACHE.values().stream().collect(Collectors.toList());
	}

	public static String getName(long id) {
		TableSchema tableSchema = TABLE_CACHE.values().stream().filter(t -> t.getId() == id).findFirst().get();
		if (null != tableSchema) {
			return tableSchema.getName();
		}

		return "";
	}

}
