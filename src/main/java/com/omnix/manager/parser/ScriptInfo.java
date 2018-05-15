package com.omnix.manager.parser;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.script.Invocable;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "SCRIPT_INFO", uniqueConstraints = { @UniqueConstraint(columnNames = { "table_id", "name" }) })
public class ScriptInfo {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private long id;

	/** 테이블 */
	@ManyToOne
	@JoinColumn(name = "table_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TableSchema tableSchema;

	/** 이름 */
	@Column(name = "name", length = 50, nullable = false)
	private String name;

	/** script */
	@Column(name = "script", length = 65535, nullable = false)
	private String script;

	/** 최근 수정일 */
	@Column(name = "modify_date", nullable = false)
	private LocalDateTime modifyDate;

	/** 설명 */
	@Column(name = "description", length = 255)
	private String description;

	/** 기본설정 */
	@Column(name = "predefine", updatable = false)
	private boolean predefine = false;

	@Transient
	@JsonIgnore
	private Invocable invocable;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public TableSchema getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(TableSchema tableSchema) {
		this.tableSchema = tableSchema;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public LocalDateTime getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(LocalDateTime modifyDate) {
		this.modifyDate = modifyDate;
	}

	public Invocable getInvocable() {
		return invocable;
	}

	public void setInvocable(Invocable invocable) {
		this.invocable = invocable;
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
