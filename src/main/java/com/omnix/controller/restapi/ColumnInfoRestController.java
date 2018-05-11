package com.omnix.controller.restapi;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.omnix.config.UiValidationException;
import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.ColumnInfoManager;
import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.parser.TableSchemaManager;

@RestController
@RequestMapping("/restapi/columnInfo")
public class ColumnInfoRestController {
	@Autowired
	private ColumnInfoManager columnInfoManager;
	@Autowired
	private TableSchemaManager tableSchemaManager;

	@RequestMapping("{tableId}/list")
	@ResponseBody
	public List<ColumnInfo> getList(@PathVariable("tableId") long tableId) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		return ColumnInfoManager.getColumnInfoCacheList(optional.get());
	}

	@RequestMapping(value = "{tableId}/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Optional<ColumnInfo> getColumn(@PathVariable("tableId") long tableId, @PathVariable("id") long id) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		return columnInfoManager.getColumnInfo(id);
	}

	@RequestMapping(value = "{tableId}/add", method = RequestMethod.POST)
	@ResponseBody
	public ColumnInfo addColumn(@PathVariable("tableId") long tableId, ColumnInfo newColumnInfo) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		newColumnInfo.setTableSchema(optional.get());
		return columnInfoManager.saveColumnInfo(newColumnInfo);
	}

	@RequestMapping(value = "{tableId}/edit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public ColumnInfo editColumn(@PathVariable("tableId") long tableId, @PathVariable("id") long id, ColumnInfo newColumnInfo) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		Optional<ColumnInfo> optional2 = columnInfoManager.getColumnInfo(id);
		if (!optional2.isPresent()) {
			throw new UiValidationException("ALERT_COLUMN_NULL");
		}
		
		if (optional2.get().isPredefine()) {
			throw new UiValidationException("ALERT_COLUMN_PREDEFINE_EDIT");
		}
		
		newColumnInfo.setId(id);
		return columnInfoManager.saveColumnInfo(newColumnInfo);
	}

	@RequestMapping(value = "{tableId}/del/{id}", method = RequestMethod.POST)
	@ResponseBody
	public boolean delColumn(@PathVariable("tableId") long tableId, @PathVariable("id") long id) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		ColumnInfo columnInfo = columnInfoManager.getColumnInfoForUpdate(id);
		if (null == columnInfo) {
			throw new UiValidationException("ALERT_COLUMN_NULL");
		}
		
		if (columnInfo.isPredefine()) {
			throw new UiValidationException("ALERT_COLUMN_PREDEFINE_EDIT");
		}

		columnInfoManager.deleteColumnInfo(columnInfo);
		return true;
	}
}
