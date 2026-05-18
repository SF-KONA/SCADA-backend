package com.example.demo.domain.repository;

import com.example.demo.domain.entity.EnvironmentMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnvironmentMeasurementRepository extends JpaRepository<EnvironmentMeasurement, Long> {

    @Query("SELECT e FROM EnvironmentMeasurement e WHERE e.paramId = :paramId ORDER BY e.measuredAt DESC LIMIT 1")
    java.util.Optional<EnvironmentMeasurement> findLatestByParamId(@Param("paramId") Long paramId);

    @Query("""
        SELECT e FROM EnvironmentMeasurement e
        WHERE e.paramId IN :paramIds
        AND e.measuredAt = (
            SELECT MAX(e2.measuredAt) FROM EnvironmentMeasurement e2 WHERE e2.paramId = e.paramId
        )
    """)
    List<EnvironmentMeasurement> findLatestByParamIds(@Param("paramIds") List<Long> paramIds);

    List<EnvironmentMeasurement> findTop7ByParamIdOrderByMeasuredAtDesc(Long paramId);
}
