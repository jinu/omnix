package com.omnix.controller;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/search")
public class SearchController {

	@RequestMapping("/list")
	public String list(ModelMap modelMap) {
		
		String key = RandomStringUtils.randomNumeric(10);
		modelMap.addAttribute("key", key);
		
		return "/search/list";
	}

}
