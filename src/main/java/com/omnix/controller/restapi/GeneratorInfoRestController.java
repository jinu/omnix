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
import com.omnix.manager.generator.GeneratorInfo;
import com.omnix.manager.generator.GeneratorManager;
import com.omnix.manager.repository.GeneratorInfoRepository;
import com.omnix.util.ParameterParser;

@RestController
@RequestMapping("/restapi/generator")
public class GeneratorInfoRestController {
	@Autowired
	private GeneratorManager generatorManager;
	@Autowired
	private GeneratorInfoRepository generatorInfoRepository;

	@RequestMapping("/list")
	@ResponseBody
	public List<GeneratorInfo> getList() {
		return generatorInfoRepository.findAll();
	}

	@RequestMapping("/add")
	@ResponseBody
	public GeneratorInfo addGeneratorInfo(GeneratorInfo generatorInfo) {
		return generatorInfoRepository.saveAndFlush(generatorInfo);
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Optional<GeneratorInfo> addGeneratorInfo(@PathVariable("id") long generatorId) {
		return generatorInfoRepository.findById(generatorId);
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.POST)
	@ResponseBody
	public GeneratorInfo addGeneratorInfoAction(@PathVariable("id") long generatorId, ParameterParser parser) {
		GeneratorInfo generatorInfo = generatorInfoRepository.getOne(generatorId);
		if (null == generatorInfo) {
			throw new UiValidationException("ALERT_GENERATOR_NULL");
		}

		generatorInfo.setName(parser.get("name"));
		generatorInfo.setContent(parser.get("content"));

		return generatorInfoRepository.saveAndFlush(generatorInfo);
	}

	@RequestMapping("/run")
	@ResponseBody
	public boolean run(ParameterParser parser) {
		return generatorManager.run(parser.getLong("tableId"), parser.getLong("scriptId"), parser.getLong("id"));
	}

	@RequestMapping("/stop")
	@ResponseBody
	public boolean stop(ParameterParser parser) {
		generatorManager.stop();
		return true;
	}
}
