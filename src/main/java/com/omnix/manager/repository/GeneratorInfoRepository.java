package com.omnix.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.omnix.manager.generator.GeneratorInfo;

public interface GeneratorInfoRepository extends JpaRepository<GeneratorInfo, Long>, JpaSpecificationExecutor<GeneratorInfo> {

}
