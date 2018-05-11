package com.omnix.controller.restapi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.omnix.config.UiValidationException;
import com.omnix.manager.parser.ScriptInfo;
import com.omnix.manager.parser.ScriptInfoManager;
import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.parser.TableSchemaManager;
import com.omnix.manager.recieve.bulk.BulkManager;
import com.omnix.util.ParameterParser;

@RestController
@RequestMapping("/restapi/bulk")
public class BulkRestController {
	@Autowired
	private BulkManager bulkManager;

	@RequestMapping("/info")
	@ResponseBody
	public Map<String, Object> bulkTargetInfoList() {
		Map<String, Object> map = new LinkedHashMap<>();

		map.put("run", bulkManager.getRun().get());
		map.put("tableSchema", bulkManager.getTableSchema());
		map.put("scriptInfo", bulkManager.getScriptInfo());
		map.put("bulkTargetList", bulkManager.getBulkTargetList());

		return map;
	}

	@RequestMapping("/prepare")
	@ResponseBody
	public boolean prepare(ParameterParser parser) {
		long tableId = parser.getLong("tableId");
		long scriptId = parser.getLong("scriptId");
		String targetPath = parser.get("path");

		TableSchema tableSchema = TableSchemaManager.getTableSchemaCache(tableId);
		if (null == tableSchema) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		ScriptInfo scriptInfo = ScriptInfoManager.getScriptInfoCache(tableId, scriptId);
		if (null == scriptInfo) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		try {
			return bulkManager.prepare(tableSchema, scriptInfo, targetPath);
		} catch (Exception e) {
			throw new UiValidationException("ALERT_BULK_PATH");
		}
	}

	@RequestMapping("/run")
	@ResponseBody
	public boolean run() {
		return bulkManager.run();
	}

	@RequestMapping("/stop")
	@ResponseBody
	public boolean stop() {
		return bulkManager.stop();
	}

}
