package com.omnix.manager.parser;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "PARSER_INFO")
public class ParserInfo {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private long id;

	/** 테이블 */
	@ManyToOne
	@JoinColumn(name = "script_info_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScriptInfo scriptInfo;

	/** 언어셋 */
	@Column(name = "encoding", length = 50, nullable = false)
	private String encoding;

	/** 장비 ip */
	@Column(name = "ip", length = 50, nullable = false, unique = true)
	private String ip;

	/** 설명 */
	@Column(name = "description", length = 255, nullable = false)
	private String description;

	/** 기본설정 */
	@Column(name = "predefine", updatable = false)
	private boolean predefine = false;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ScriptInfo getScriptInfo() {
		return scriptInfo;
	}

	public void setScriptInfo(ScriptInfo scriptInfo) {
		this.scriptInfo = scriptInfo;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPredefine() {
		return predefine;
	}

	public void setPredefine(boolean predefine) {
		this.predefine = predefine;
	}

}
