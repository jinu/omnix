package com.omnix.manager.websocket;

import com.omnix.manager.recieve.LogBean;

/**
 * 실시간 로그 처리용
 */
public class RealtimeLog {
	private String ip;
	private String text;
	
	public RealtimeLog() {
	}

	public RealtimeLog(LogBean logBean) {
		this.ip = logBean.getRemoteAddr();
		this.text = logBean.getText();
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
