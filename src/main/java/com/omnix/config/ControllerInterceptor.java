package com.omnix.config;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * controller 처리 interceptor
 */
@Component
public class ControllerInterceptor extends HandlerInterceptorAdapter {
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		
		if (modelAndView == null) {
			return;
		}

		View view = modelAndView.getView();
		String viewName = modelAndView.getViewName();
		if (view instanceof org.springframework.web.servlet.view.RedirectView || (null != viewName && viewName.startsWith("redirect:")) || (null != viewName && viewName.equals(":move")) || (null != viewName && viewName.equals(":exit"))) {
            return;
        }

		URL url = new URL(request.getRequestURL().toString());
		String depths = url.getPath();

		if (StringUtils.startsWith(depths, "/error")) {
			return;
		}
		
		String[] depthsArray = StringUtils.splitPreserveAllTokens(depths, "/");
		String depth1 = depthsArray[1];
		String depth2 = depthsArray.length > 2 ? depthsArray[2] : "";
		String depth3 = depthsArray.length > 3 ? depthsArray[3] : "";
		String depth4 = depthsArray.length > 4 ? depthsArray[4] : "";
		String depth5 = depthsArray.length > 5 ? depthsArray[5] : "";
		
		if (!StringUtils.equals("restapi", depth1)) {
			modelAndView.addObject("depth1", depth1);
			modelAndView.addObject("depth2", depth2);
			modelAndView.addObject("depth3", depth3);
			modelAndView.addObject("depth4", depth4);
			modelAndView.addObject("depth5", depth5);
			modelAndView.addObject("depths", depths);
			modelAndView.addObject("currentUrl", url.getFile());
		}
		
		long jobId = RandomUtils.nextLong(0, 65535);
		modelAndView.addObject("jobId", jobId);
	}
}
