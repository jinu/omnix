package com.omnix.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.omnix.manager.parser.TableSchema;

public interface TableSchemaRepository extends JpaRepository<TableSchema, Long>, JpaSpecificationExecutor<TableSchema> {

}
