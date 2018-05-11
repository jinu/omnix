package com.omnix.controller.restapi;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.omnix.config.UiValidationException;
import com.omnix.manager.parser.ParserInfo;
import com.omnix.manager.parser.ParserInfoManager;
import com.omnix.manager.parser.ScriptInfo;
import com.omnix.manager.parser.ScriptInfoManager;

@RestController
@RequestMapping("/restapi/parserInfo")
public class ParserInfoRestController {
	@Autowired
	private ParserInfoManager parserInfoManager;
	@Autowired
	private ScriptInfoManager scriptInfoManager;

	@RequestMapping("/{tableId}/list")
	@ResponseBody
	public List<ParserInfo> getList(@PathVariable("tableId") long tableId) {
		return parserInfoManager.getPaserInfoListByTableId(tableId);
	}

	@RequestMapping("/{tableId}/{scriptId}/list")
	@ResponseBody
	public List<ParserInfo> getList(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId) {
		return parserInfoManager.getPaserInfoListByScriptId(tableId, scriptId);
	}

	@RequestMapping(value = "/{tableId}/{scriptId}/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Optional<ParserInfo> getParserList(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId, @PathVariable("id") long id) {
		Optional<ScriptInfo> optional = scriptInfoManager.getScriptInfo(scriptId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		if (null == optional.get().getTableSchema() || optional.get().getTableSchema().getId() != tableId) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		return parserInfoManager.getParserInfo(id);
	}

	@RequestMapping(value = "/{tableId}/{scriptId}/add", method = RequestMethod.POST)
	@ResponseBody
	public ParserInfo addParser(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId, ParserInfo parserInfo) {
		Optional<ScriptInfo> optional = scriptInfoManager.getScriptInfo(scriptId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		if (null == optional.get().getTableSchema() || optional.get().getTableSchema().getId() != tableId) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		try {
			parserInfoManager.parseIp(parserInfo.getIp());

		} catch (UnknownHostException e) {
			throw new UiValidationException("ALERT_PARSERINFO_IP");
		}

		parserInfo.setScriptInfo(optional.get());
		return parserInfoManager.saveParserInfo(parserInfo);
	}

	@RequestMapping(value = "/{tableId}/{scriptId}/edit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public ParserInfo editMapping(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId, @PathVariable("id") long id, ParserInfo newParserInfo) {
		Optional<ParserInfo> optional = parserInfoManager.getParserInfo(id);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_PARSERINFO_NULL");
		}

		if (optional.get().isPredefine()) {
			throw new UiValidationException("ALERT_PARSERINFO_PREDEFINE_EDIT");
		}

		if (null == optional.get().getScriptInfo() || optional.get().getScriptInfo().getId() != scriptId) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		try {
			parserInfoManager.parseIp(newParserInfo.getIp());

		} catch (UnknownHostException e) {
			throw new UiValidationException("ALERT_PARSERINFO_IP");
		}

		newParserInfo.setId(id);
		newParserInfo.setScriptInfo(optional.get().getScriptInfo());

		return parserInfoManager.saveParserInfo(newParserInfo);
	}

	@RequestMapping(value = "/{tableId}/{scriptId}/del/{id}", method = RequestMethod.POST)
	@ResponseBody
	public boolean delColumn(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId, @PathVariable("id") long id) {
		ParserInfo parserInfo = parserInfoManager.getParserInfoForUpdate(id);
		if (null == parserInfo) {
			throw new UiValidationException("ALERT_PARSERINFO_NULL");
		}

		if (parserInfo.isPredefine()) {
			throw new UiValidationException("ALERT_PARSERINFO_PREDEFINE_EDIT");
		}

		if (null == parserInfo.getScriptInfo() || parserInfo.getScriptInfo().getId() != scriptId) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		parserInfoManager.deleteParserInfo(parserInfo);
		return true;
	}
}
