package com.example.demo.domain.repository;

import com.example.demo.domain.entity.EnvironmentParameter;
import com.example.demo.domain.enums.EnvironmentParamCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvironmentParameterRepository extends JpaRepository<EnvironmentParameter, Long> {

    // 카테고리별 환경 파라미터 조회
    List<EnvironmentParameter> findByParamCategory(EnvironmentParamCategory category);

    // 센서 ID별 환경 파라미터 조회
    List<EnvironmentParameter> findBySensorId(String sensorId);
}