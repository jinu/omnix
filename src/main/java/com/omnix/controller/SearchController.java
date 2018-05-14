package com.omnix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/search")
public class SearchController {

	@RequestMapping("/list")
	public String list(ModelMap modelMap) {
		
		
		return "/search/list";
	}

}
