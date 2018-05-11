package com.omnix.manager.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.omnix.manager.parser.ColumnInfo;

public interface ColumnInfoRepository extends JpaRepository<ColumnInfo, Long>, JpaSpecificationExecutor<ColumnInfo> {
	public List<ColumnInfo> findAllByTableSchemaId(long id, Sort sort);
}
