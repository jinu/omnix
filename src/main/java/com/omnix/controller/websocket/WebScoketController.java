package com.omnix.controller.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.omnix.manager.websocket.RealtimeLogRegister;
import com.omnix.manager.websocket.WebSocketManager;

@Controller
public class WebScoketController {
	@Autowired
	private WebSocketManager webSocketManager;

	@MessageMapping("/realtimeLog/register")
	public boolean registerLog(RealtimeLogRegister realtimeLogRegister) {
		webSocketManager.realtimeLogRegister(realtimeLogRegister);
		return true;
	}

	@MessageMapping("/realtimeLog/unRegister")
	public boolean unRegisterLog(RealtimeLogRegister realtimeLogRegister) {
		webSocketManager.realtimeLogUnRegister(realtimeLogRegister);
		return true;
	}
}
