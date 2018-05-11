package com.omnix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tableInfo")
public class TableController {

	@RequestMapping("/list")
	public String list(ModelMap modelMap) {
		return "/tableInfo/list";
	}

	@RequestMapping("/add")
	public String add(ModelMap modelMap) {
		modelMap.addAttribute("mode", "add");
		return "/tableInfo/add";
	}

	@RequestMapping(value = { "/edit/{id}" })
	public String edit(ModelMap modelMap, @PathVariable("id") long id) {
		modelMap.addAttribute("id", id);
		modelMap.addAttribute("mode", "edit");
		return "/tableInfo/add";
	}

}
