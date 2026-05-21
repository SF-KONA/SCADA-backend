package com.example.demo.domain.repository;

import com.example.demo.domain.entity.EnvironmentMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    List<EnvironmentMeasurement> findByParamIdAndMeasuredAtAfterOrderByMeasuredAtAsc(
            Long paramId, LocalDateTime after);

    List<EnvironmentMeasurement> findTop13ByParamIdOrderByMeasuredAtDesc(Long paramId);

    // ★ 추가 1 — 현재 시각 이전 최신 13개
    @Query("SELECT e FROM EnvironmentMeasurement e WHERE e.paramId = :paramId AND e.measuredAt <= :now ORDER BY e.measuredAt DESC LIMIT 13")
    List<EnvironmentMeasurement> findTop13ByParamIdBeforeNow(@Param("paramId") Long paramId, @Param("now") LocalDateTime now);

    // ★ 추가 2 — 현재 시각 이전 각 paramId별 최신값
    @Query("""
        SELECT e FROM EnvironmentMeasurement e
        WHERE e.paramId IN :paramIds
        AND e.measuredAt <= :now
        AND e.measuredAt = (
            SELECT MAX(e2.measuredAt) FROM EnvironmentMeasurement e2
            WHERE e2.paramId = e.paramId AND e2.measuredAt <= :now
        )
    """)
    List<EnvironmentMeasurement> findLatestByParamIdsBeforeNow(@Param("paramIds") List<Long> paramIds, @Param("now") LocalDateTime now);
}