package com.omnix.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.omnix.manager.parser.ScriptInfo;

public interface ScriptInfoRepository extends JpaRepository<ScriptInfo, Long>, JpaSpecificationExecutor<ScriptInfo> {

}
