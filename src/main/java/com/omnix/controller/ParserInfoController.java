package com.omnix.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.parser.TableSchemaManager;

@Controller
@RequestMapping("/parserInfo")
public class ParserInfoController {

	@RequestMapping("/list/{id}")
	public String list(ModelMap modelMap, @PathVariable("id") long id) {

		List<TableSchema> tableList = TableSchemaManager.getListCache();
		modelMap.addAttribute("tableList", tableList);
		modelMap.addAttribute("tableId", id);
		modelMap.addAttribute("mode", "list");
		return "/parserInfo/list";
	}

	@RequestMapping("/add/{id}")
	public String add(ModelMap modelMap, @PathVariable("id") long id) {
		modelMap.addAttribute("tableId", id);
		modelMap.addAttribute("mode", "add");
		return "/parserInfo/add";
	}

	@RequestMapping("/edit/{id}/{scriptId}/{parserId}")
	public String edit(ModelMap modelMap, @PathVariable("id") long id, @PathVariable("scriptId") long scriptId, @PathVariable("parserId") long parserId) {
		modelMap.addAttribute("tableId", id);
		modelMap.addAttribute("scriptId", scriptId);
		modelMap.addAttribute("parserId", parserId);
		modelMap.addAttribute("mode", "edit");
		return "/parserInfo/add";
	}

}
