package com.omnix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
	@RequestMapping(value = "")
	public String home(ModelMap modelMap) {
		return "home";
	}
	
	@RequestMapping(value = "/sample")
	public String sample(ModelMap modelMap) {
		return "sample";
	}
}
