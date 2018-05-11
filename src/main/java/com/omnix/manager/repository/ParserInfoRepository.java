package com.omnix.manager.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.omnix.manager.parser.ParserInfo;

public interface ParserInfoRepository extends JpaRepository<ParserInfo, Long>, JpaSpecificationExecutor<ParserInfo> {
	public List<ParserInfo> findAllByScriptInfoIdAndScriptInfoTableSchemaId(long scriptId, long tableId, Sort sort);
	public List<ParserInfo> findAllByScriptInfoTableSchemaId(long id, Sort sort);
}
