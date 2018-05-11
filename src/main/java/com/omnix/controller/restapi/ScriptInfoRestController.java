package com.omnix.controller.restapi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.script.Invocable;
import javax.script.ScriptException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.omnix.config.UiValidationException;
import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.ColumnInfoManager;
import com.omnix.manager.parser.ScriptInfo;
import com.omnix.manager.parser.ScriptInfoManager;
import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.parser.TableSchemaManager;
import com.omnix.manager.recieve.LogBean;
import com.omnix.util.ParameterParser;

@RestController
@RequestMapping("/restapi/scriptInfo")
public class ScriptInfoRestController {
	@Autowired
	private ScriptInfoManager scriptInfoManager;
	@Autowired
	private TableSchemaManager tableSchemaManager;

	@RequestMapping("{tableId}/list")
	@ResponseBody
	public List<ScriptInfo> getList(@PathVariable("tableId") long tableId) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		return ScriptInfoManager.getScriptInfoCache(optional.get());
	}

	@RequestMapping(value = "{tableId}/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Optional<ScriptInfo> getScript(@PathVariable("tableId") long tableId, @PathVariable("id") long id) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		return scriptInfoManager.getScriptInfo(id);
	}

	@RequestMapping(value = "{tableId}/add", method = RequestMethod.POST)
	@ResponseBody
	public ScriptInfo addScript(@PathVariable("tableId") long tableId, ScriptInfo newScriptInfo) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		newScriptInfo.setTableSchema(optional.get());
		newScriptInfo.setModifyDate(LocalDateTime.now());
		try {
			return scriptInfoManager.saveScriptInfo(newScriptInfo);

		} catch (ScriptException e) {
			throw new UiValidationException(e.getMessage());
		}
	}

	@RequestMapping(value = "{tableId}/edit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public ScriptInfo editColumn(@PathVariable("tableId") long tableId, @PathVariable("id") long id, ScriptInfo newScriptInfo) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		Optional<ScriptInfo> optional2 = scriptInfoManager.getScriptInfo(id);
		if (!optional2.isPresent()) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		if (optional2.get().isPredefine()) {
			throw new UiValidationException("ALERT_SCRIPT_PREDEFINE_EDIT");
		}

		newScriptInfo.setId(id);
		newScriptInfo.setTableSchema(optional.get());
		newScriptInfo.setModifyDate(LocalDateTime.now());
		try {
			return scriptInfoManager.saveScriptInfo(newScriptInfo);

		} catch (ScriptException e) {
			throw new UiValidationException(e.getMessage());
		}
	}

	@RequestMapping(value = "{tableId}/del/{id}", method = RequestMethod.POST)
	@ResponseBody
	public boolean delColumn(@PathVariable("tableId") long tableId, @PathVariable("id") long id) {
		Optional<TableSchema> optional = tableSchemaManager.getTableSchema(tableId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		ScriptInfo scriptInfo = scriptInfoManager.getScriptInfoForUpdate(id);
		if (null == scriptInfo) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		if (scriptInfo.isPredefine()) {
			throw new UiValidationException("ALERT_SCRIPT_PREDEFINE_EDIT");
		}

		scriptInfoManager.deleteScriptInfo(scriptInfo);
		return true;
	}

	@RequestMapping(value = "{tableId}/test", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> scriptTest(@PathVariable("tableId") long tableId, ParameterParser parser) {
		String log = parser.get("log");
		String script = parser.get("script");

		Map<String, ColumnInfo> columnInfoMap = ColumnInfoManager.getColumnInfoCache(tableId);

		if (null == columnInfoMap) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		List<Map<String, Object>> lists = new ArrayList<>();
		try {
			Invocable invocable = scriptInfoManager.getInvocable(script);

			LogBean logBean = new LogBean(0, 0, log, null);
			invocable.invokeFunction("parseLog", logBean, true);

			int keySize = logBean.getKeyList().size();
			for (int i = 0; i < keySize; i++) {

				String fieldName = logBean.getKey(i);
				String logText = logBean.getValue(i);
				ColumnInfo columnInfo = columnInfoMap.get(fieldName);

				Map<String, Object> map = new LinkedHashMap<>();
				map.put("key", fieldName);
				map.put("value", logText);
				map.put("columnInfo", columnInfo);

				lists.add(map);
			}

			Map<String, Object> resultMap = Collections.emptyMap();
			resultMap.put("result", true);
			resultMap.put("lists", lists);

			return resultMap;

		} catch (NoSuchMethodException | ScriptException e) {

			String[] message = { e.getMessage() };
			throw new UiValidationException("ALERT_SCRIPT_PARSE", message);
		}
	}
}
