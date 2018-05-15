package com.omnix.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.omnix.config.UiValidationException;
import com.omnix.manager.parser.ScriptInfo;
import com.omnix.manager.parser.ScriptInfoManager;
import com.omnix.manager.recieve.file.FileMonitoringInfo;
import com.omnix.manager.recieve.file.FileMonitoringManager;

@RestController
@RequestMapping("/restapi/fileMonitoring")
public class FileMonitoringRestController {
	@Autowired
	private FileMonitoringManager fileMonitoringManager;
	@Autowired
	private ScriptInfoManager scriptInfoManager;

	@RequestMapping("/{tableId}/list")
	@ResponseBody
	public List<FileMonitoringInfo> getList(@PathVariable("tableId") long tableId) {
		return fileMonitoringManager.getFileMonitoringInfoListByTableId(tableId);
	}

	@RequestMapping("/{tableId}/{scriptId}/list")
	@ResponseBody
	public List<FileMonitoringInfo> getList(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId) {
		return fileMonitoringManager.getFileMonitoringInfoListByScriptId(tableId, scriptId);
	}

	@RequestMapping(value = "/{tableId}/{scriptId}/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Optional<FileMonitoringInfo> getFileMonitoringList(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId, @PathVariable("id") long id) {
		Optional<ScriptInfo> optional = scriptInfoManager.getScriptInfo(scriptId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		if (null == optional.get().getTableSchema() || optional.get().getTableSchema().getId() != tableId) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		return fileMonitoringManager.getFileMonitoringInfo(id);
	}

	@RequestMapping(value = "/{tableId}/{scriptId}/add", method = RequestMethod.POST)
	@ResponseBody
	public FileMonitoringInfo addFileMonitoring(@PathVariable("tableId") long tableId, @PathVariable("scriptId") long scriptId, FileMonitoringInfo fileMonitoringInfo) {
		Optional<ScriptInfo> optional = scriptInfoManager.getScriptInfo(scriptId);
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_SCRIPT_NULL");
		}

		if (null == optional.get().getTableSchema() || optional.get().getTableSchema().getId() != tableId) {
			throw new UiValidationException("ALERT_TABLE_NULL");
		}

		fileMonitoringInfo.setScriptInfo(optional.get());
		return fileMonitoringManager.saveFileMonitoringInfo(fileMonitoringInfo);
	}
}
