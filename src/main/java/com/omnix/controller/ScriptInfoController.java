package com.omnix.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.parser.TableSchemaManager;

@Controller
@RequestMapping("/scriptInfo")
public class ScriptInfoController {
	
	@RequestMapping("/list/{id}")
	public String list(ModelMap modelMap, @PathVariable("id") long id) {
		
		List<TableSchema> tableList = TableSchemaManager.getListCache();
		modelMap.addAttribute("tableList", tableList);
		modelMap.addAttribute("tableId", id);
		modelMap.addAttribute("mode", "list");
		return "/scriptInfo/list";
	}
	
	@RequestMapping("/add/{id}")
	public String add(ModelMap modelMap, @PathVariable("id") long id) {
		modelMap.addAttribute("tableId", id);
		modelMap.addAttribute("mode", "add");
		return "/scriptInfo/add";
	}
	
	@RequestMapping("/edit/{id}/{scriptId}")
	public String edit(ModelMap modelMap, @PathVariable("id") long id, @PathVariable("scriptId") long scriptId) {
		modelMap.addAttribute("tableId", id);
		modelMap.addAttribute("scriptId", scriptId);
		modelMap.addAttribute("mode", "edit");
		return "/scriptInfo/add";
	}
	
	
}
