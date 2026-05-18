package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Alarm;
import com.example.demo.domain.enums.AlarmSeverity;
import com.example.demo.domain.enums.AlarmSourceType;
import com.example.demo.domain.enums.AlarmStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    // ── 팀원 기존 코드 (메서드명 그대로 유지) ──────────────────

    @Query("""
        SELECT a FROM Alarm a
        JOIN EquipmentParameter ep ON ep.paramId = a.equipmentParamId
        WHERE ep.equipmentId = :equipmentId
        AND a.status IN ('NEW', 'ACK', 'IN_PROGRESS')
        ORDER BY a.occurredAt DESC
    """)
    List<Alarm> findActiveAlarmsByEquipmentId(@Param("equipmentId") String equipmentId);

    @Query("""
        SELECT a FROM Alarm a
        JOIN EquipmentParameter ep ON ep.paramId = a.equipmentParamId
        WHERE ep.equipmentId = :equipmentId
        ORDER BY a.occurredAt DESC
    """)
    List<Alarm> findAllAlarmsByEquipmentId(@Param("equipmentId") String equipmentId);

    // ── 알람 센터용 추가 쿼리 ──────────────────────────────────

    /**
     * 알람 목록 조회 (다중 필터 + 페이지네이션)
     *
     * EQP 알람: equipmentParamId → EquipmentParameter → Equipment 조인으로 설비/공정 필터
     * ENV 알람: EnvironmentParameter에 stepNo/zoneCode 없으므로 sourceType=ENV 단독 필터만 지원
     *           (stepNo 필터 시 ENV 알람은 자동 제외됨)
     */
    @Query(value = """
            SELECT a FROM Alarm a
            LEFT JOIN EquipmentParameter ep ON a.equipmentParamId = ep.paramId
            LEFT JOIN Equipment eq ON ep.equipmentId = eq.equipmentId
            WHERE (:sourceType IS NULL OR a.sourceType = :sourceType)
              AND (:severities IS NULL OR a.severity IN :severities)
              AND (:statuses IS NULL OR a.status IN :statuses)
              AND (:equipmentId IS NULL OR eq.equipmentId = :equipmentId)
              AND (:stepNo IS NULL OR eq.stepNo = :stepNo)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to IS NULL OR a.occurredAt <= :to)
            ORDER BY a.occurredAt DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM Alarm a
            LEFT JOIN EquipmentParameter ep ON a.equipmentParamId = ep.paramId
            LEFT JOIN Equipment eq ON ep.equipmentId = eq.equipmentId
            WHERE (:sourceType IS NULL OR a.sourceType = :sourceType)
              AND (:severities IS NULL OR a.severity IN :severities)
              AND (:statuses IS NULL OR a.status IN :statuses)
              AND (:equipmentId IS NULL OR eq.equipmentId = :equipmentId)
              AND (:stepNo IS NULL OR eq.stepNo = :stepNo)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to IS NULL OR a.occurredAt <= :to)
            """)
    Page<Alarm> findByFilters(
            @Param("sourceType") AlarmSourceType sourceType,
            @Param("severities") List<AlarmSeverity> severities,
            @Param("statuses") List<AlarmStatus> statuses,
            @Param("equipmentId") String equipmentId,
            @Param("stepNo") String stepNo,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /**
     * 미확인 비상 알람 조회 (severity=ERR, status=NEW)
     */
    @Query("""
            SELECT a FROM Alarm a
            WHERE a.severity = 'ERR' AND a.status = 'NEW'
            ORDER BY a.occurredAt DESC
            """)
    List<Alarm> findEmergencyAlarms();
}