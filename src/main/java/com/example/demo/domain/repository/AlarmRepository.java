package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Alarm;
import com.example.demo.domain.enums.AlarmStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    // 특정 설비의 활성 알람 조회 (equipmentParamId 기준)
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
}