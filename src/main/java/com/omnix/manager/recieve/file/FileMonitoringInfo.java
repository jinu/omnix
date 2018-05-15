package com.omnix.manager.recieve.file;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.omnix.manager.parser.ScriptInfo;

@Entity
@Table(name = "FILEMONITORING_INFO")
public class FileMonitoringInfo {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private long id;

	@Column(name = "path", nullable = false, unique = true)
	private String path;

	@ManyToOne
	@JoinColumn(name = "script_info_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ScriptInfo scriptInfo;

	@Column(name = "enable")
	private boolean enable;

	/** 최근 수정일 */
	@Column(name = "modify_date", nullable = false)
	private LocalDateTime modifyDate;

	/** 설명 */
	@Column(name = "description", length = 255)
	private String description;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public ScriptInfo getScriptInfo() {
		return scriptInfo;
	}

	public void setScriptInfo(ScriptInfo scriptInfo) {
		this.scriptInfo = scriptInfo;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public LocalDateTime getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(LocalDateTime modifyDate) {
		this.modifyDate = modifyDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
