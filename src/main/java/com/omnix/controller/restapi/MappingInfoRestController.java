package com.omnix.controller.restapi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.omnix.config.UiValidationException;
import com.omnix.manager.parser.MappingInfo;
import com.omnix.manager.parser.MappingInfoManager;

@RestController
@RequestMapping("/restapi/mappingInfo")
public class MappingInfoRestController {
	@Autowired
	private MappingInfoManager mappingInfoManager;

	@RequestMapping("/list")
	@ResponseBody
	public List<MappingInfo> getList() {
		return mappingInfoManager.getMappingInfoList();
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Optional<MappingInfo> getMappingList(@PathVariable("id") long id) {
		return mappingInfoManager.getMappingInfo(id);
	}

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	@ResponseBody
	public MappingInfo addMapping(@PathVariable("tableId") long tableId, MappingInfo mappingInfo) {
		mappingInfo.setModifyDate(LocalDateTime.now());
		return mappingInfoManager.saveMappingInfo(mappingInfo);
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public MappingInfo editMapping(@PathVariable("id") long id, MappingInfo newMappingInfo) {
		Optional<MappingInfo> optional = mappingInfoManager.getMappingInfo(newMappingInfo.getId());
		if (!optional.isPresent()) {
			throw new UiValidationException("ALERT_MAPPING_NULL");
		}
		
		if (optional.get().isPredefine()) {
			throw new UiValidationException("ALERT_MAPPING_PREDEFINE_EDIT");
		}
		
		newMappingInfo.setModifyDate(LocalDateTime.now());
		return mappingInfoManager.saveMappingInfo(newMappingInfo);
	}

	@RequestMapping(value = "/del/{id}", method = RequestMethod.POST)
	@ResponseBody
	public boolean delMapping(@PathVariable("id") long id) {

		MappingInfo mappingInfo = mappingInfoManager.getMappingInfoForUpdate(id);
		if (null == mappingInfo) {
			throw new UiValidationException("ALERT_MAPPING_NULL");
		}
		
		if (mappingInfo.isPredefine()) {
			throw new UiValidationException("ALERT_MAPPING_PREDEFINE_EDIT");
		}

		mappingInfoManager.deleteMappingInfo(mappingInfo);
		return true;
	}

}
