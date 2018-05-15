package com.omnix.manager.parser;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "TABLE_SHCEMA")
public class TableSchema {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", updatable = false)
	private long id;

	@Column(name = "name", unique = true, length = 20, nullable = false)
	private String name;

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
