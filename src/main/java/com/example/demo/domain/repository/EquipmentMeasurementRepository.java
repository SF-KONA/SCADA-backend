package com.example.demo.domain.repository;

import com.example.demo.domain.entity.EquipmentMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EquipmentMeasurementRepository extends JpaRepository<EquipmentMeasurement, Long> {

    List<EquipmentMeasurement> findByParamIdAndMeasuredAtAfterOrderByMeasuredAtAsc(
            Long paramId, LocalDateTime after);

    @Query("SELECT m FROM EquipmentMeasurement m WHERE m.paramId = :paramId ORDER BY m.measuredAt DESC LIMIT 1")
    java.util.Optional<EquipmentMeasurement> findLatestByParamId(@Param("paramId") Long paramId);

    // ★ 추가 — 현재 시각 이전 데이터 중 가장 최근값
    @Query("SELECT m FROM EquipmentMeasurement m WHERE m.paramId = :paramId AND m.measuredAt <= :now ORDER BY m.measuredAt DESC LIMIT 1")
    java.util.Optional<EquipmentMeasurement> findLatestByParamIdBeforeNow(@Param("paramId") Long paramId, @Param("now") LocalDateTime now);
}