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
import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.parser.TableSchemaManager;

@RestController
@RequestMapping("/restapi/table")
public class TableSchemaRestController {
	@Autowired
	private TableSchemaManager tableSchemaManager;

	@RequestMapping("/list")
	@ResponseBody
	public List<TableSchema> getList() {
		return TableSchemaManager.getListCache();
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Optional<TableSchema> getColumn(@PathVariable("id") long id) {
		return tableSchemaManager.getTableSchema(id);
	}

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	@ResponseBody
	public TableSchema addColumn(TableSchema tableSchema) {
		return tableSchemaManager.saveTableSchema(tableSchema, true);
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public TableSchema editColumn(@PathVariable("id") long id, TableSchema newTableSchema) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(id);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		TableSchema tableSchema = optional.get();
		if (tableSchema.isPredefine()) {
			throw new UiValidationException("ALERT_TABLE_PREDEFINE_EDIT");
		}

		return tableSchemaManager.saveTableSchema(newTableSchema, false);
	}

	@RequestMapping(value = "/del/{id}", method = RequestMethod.POST)
	@ResponseBody
	public boolean delColumn(@PathVariable("id") long id) {
		TableSchema tableSchema = tableSchemaManager.getTableSchemaForUpdate(id);
		if (null == tableSchema) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}
		
		if (tableSchema.isPredefine()) {
			throw new UiValidationException("ALERT_TABLE_PREDEFINE_EDIT");
		}

		tableSchemaManager.deleteTableSchema(tableSchema);
		return true;
	}
}
