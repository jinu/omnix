package com.omnix.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.omnix.manager.parser.MappingInfo;

public interface MappingInfoRepository extends JpaRepository<MappingInfo, Long>, JpaSpecificationExecutor<MappingInfo> {

}
