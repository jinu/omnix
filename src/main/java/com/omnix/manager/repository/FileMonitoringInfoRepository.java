package com.omnix.manager.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.omnix.manager.recieve.file.FileMonitoringInfo;

public interface FileMonitoringInfoRepository extends JpaRepository<FileMonitoringInfo, Long>, JpaSpecificationExecutor<FileMonitoringInfo> {
	public List<FileMonitoringInfo> findAllByScriptInfoIdAndScriptInfoTableSchemaId(long scriptId, long tableId, Sort sort);
	public List<FileMonitoringInfo> findAllByScriptInfoTableSchemaId(long id, Sort sort);
}
