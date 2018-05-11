package com.omnix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.omnix.manager.parser.TableSchema;
import com.omnix.manager.parser.TableSchemaManager;

@Controller
@RequestMapping("/columnInfo")
public class ColumnInfoController {
	@Autowired
	private TableSchemaManager tableSchemaManager;

	@RequestMapping("/list/{id}")
	public String list(ModelMap modelMap, @PathVariable("id") long id) {

		List<TableSchema> tableList = TableSchemaManager.getListCache();
		modelMap.addAttribute("tableList", tableList);
		modelMap.addAttribute("table", tableSchemaManager.getTableSchema(id).get());
		modelMap.addAttribute("mode", "list");
		return "/columnInfo/list";
	}

	@RequestMapping("/add/{id}")
	public String add(ModelMap modelMap, @PathVariable("id") long id) {
		modelMap.addAttribute("table", tableSchemaManager.getTableSchema(id).get());
		modelMap.addAttribute("mode", "add");
		return "/columnInfo/add";
	}

	@RequestMapping("/edit/{id}/{columnId}")
	public String edit(ModelMap modelMap, @PathVariable("id") long id, @PathVariable("columnId") long columnId) {
		modelMap.addAttribute("table", tableSchemaManager.getTableSchema(id).get());
		modelMap.addAttribute("columnId", columnId);
		modelMap.addAttribute("mode", "edit");
		return "/columnInfo/add";
	}

}
