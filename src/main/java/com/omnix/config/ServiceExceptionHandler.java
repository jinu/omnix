package com.omnix.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.omnix.util.JsonUtils;

@ControllerAdvice
public class ServiceExceptionHandler {
	@Autowired
	private MessageSourceCustom messageSourceCustom;

	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@ExceptionHandler(UiValidationException.class)
	public void uiValidationException(UiValidationException ex, HttpServletResponse response, HttpServletRequest request, Locale locale) throws IOException {

		ex.printStackTrace();

		response.setHeader("Expires", "-1");
		response.setHeader("Cache-Control", "must-revalidate, no-store, no-cache");
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		response.setHeader("Pragma", "no-cache");
		response.setCharacterEncoding("UTF-8");

		PrintWriter out = response.getWriter();

		boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
		String uri = request.getRequestURI();

		if (ajax || StringUtils.startsWith(uri, "/restapi")) {
			response.setContentType("application/json; charset=UTF-8");

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("error", "Ui Error");
			map.put("status", 500);
			map.put("message", messageSourceCustom.getMessage(ex.getMessage(), ex.getArgs(), "", locale));

			out.println(JsonUtils.fromObj(map));
			out.flush();
			out.close();
			return;

		} else {
			
			response.setContentType("text/html; charset=utf-8");

			out.println("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body>");
			out.println("<script type=\"text/javascript\">");

			String message = messageSourceCustom.getMessage(ex.getMessage(), ex.getMessage(), locale);
			if (null != ex.getArgs()) {
				message = messageSourceCustom.getMessage(ex.getMessage(), ex.getArgs(), ex.getMessage(), locale);
			}

			out.println("if (self==top) {");
			out.println("alert('" + message + "');");
			out.println("} else {");
			out.println("parent.alert('" + message + "');");
			out.println("}");

			out.println("</script></body></html>");
		}

	}

}
