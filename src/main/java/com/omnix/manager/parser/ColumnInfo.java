package com.omnix.manager.parser;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * column Info
 */
@Entity
@Table(name = "column_info", uniqueConstraints = { @UniqueConstraint(columnNames = { "table_id", "name" }) })
public class ColumnInfo {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private long id;

	/** 테이블 */
	@ManyToOne
	@JoinColumn(name = "table_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TableSchema tableSchema;

	/** 컬럼 명 */
	@Column(name = "name", length = 50, nullable = false, updatable = false)
	private String name;

	/** 별칭 */
	@Column(name = "alias", length = 50)
	private String alias = "";

	/** 컬럼 타입 */
	@Column(name = "log_field_type", nullable = false, updatable = false)
	@Enumerated(value = EnumType.STRING)
	private LogFieldType logFieldType;

	/** 검색 여부 */
	@Column(name = "search")
	private boolean search = false;

	/** 통계 여부 */
	@Column(name = "statistics")
	private boolean statistics = false;

	/** 기본설정 */
	@Column(name = "predefine", updatable = false)
	private boolean predefine = false;

	/** 설명 */
	@Column(name = "description", length = 255)
	private String description;
	
	public ColumnInfo() {
	}
	
	public ColumnInfo(String name, LogFieldType logFieldType, boolean search, boolean statistics) {
		this.name = name;
		this.logFieldType = logFieldType;
		this.search = search;
		this.statistics = statistics;
	}

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

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public LogFieldType getLogFieldType() {
		return logFieldType;
	}

	public void setLogFieldType(LogFieldType logFieldType) {
		this.logFieldType = logFieldType;
	}

	public boolean isSearch() {
		return search;
	}

	public void setSearch(boolean search) {
		this.search = search;
	}

	public boolean isStatistics() {
		return statistics;
	}

	public void setStatistics(boolean statistics) {
		this.statistics = statistics;
	}

	public boolean isPredefine() {
		return predefine;
	}

	public void setPredefine(boolean predefine) {
		this.predefine = predefine;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
