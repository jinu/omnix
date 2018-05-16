package com.omnix.controller.restapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.omnix.manager.websocket.RealtimeLogRegister;
import com.omnix.manager.websocket.WebSocketManager;


@RestController
@RequestMapping("/restapi/realtimeLog")
public class RealtimeLogRegisterRestController {
	@Autowired
	private WebSocketManager webSocketManager;
	
	@RequestMapping("/register")
	@ResponseBody
	public boolean registerLog(RealtimeLogRegister realtimeLogRegister) {
		webSocketManager.realtimeLogRegister(realtimeLogRegister);
		return true;
	}

	@RequestMapping("/unRegister")
	@ResponseBody
	public boolean unRegisterLog(RealtimeLogRegister realtimeLogRegister) {
		webSocketManager.realtimeLogUnRegister(realtimeLogRegister);
		return true;
	}

}
