package com.omnix.manager.parser;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDateTime;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "mapping_info")
public class MappingInfo {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private long id;

	/** 이름 */
	@Column(name = "name", length = 50, nullable = false, updatable = false)
	private String name;

	/** script */
	@Column(name = "content", length = 65535, nullable = false)
	private String content;

	/** 최근 수정일 */
	@Column(name = "modify_date", nullable = false)
	private LocalDateTime modifyDate;

	/** 설명 */
	@Column(name = "description", length = 255)
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
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

	public boolean isPredefine() {
		return predefine;
	}

	public void setPredefine(boolean predefine) {
		this.predefine = predefine;
	}

	@Transient
	public Map<String, String> getMappingMap() {
		return MappingInfoManager.generateMapping(this.content);
	}

}
