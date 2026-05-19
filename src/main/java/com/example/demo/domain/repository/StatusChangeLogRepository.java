package com.example.demo.domain.repository;

import com.example.demo.domain.entity.StatusChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatusChangeLogRepository extends JpaRepository<StatusChangeLog, Long> {

    // 설비의 가장 최근 상태 조회
    @Query("SELECT s FROM StatusChangeLog s WHERE s.equipmentId = :equipmentId ORDER BY s.changedAt DESC LIMIT 1")
    Optional<StatusChangeLog> findLatestByEquipmentId(@Param("equipmentId") String equipmentId);

    List<StatusChangeLog> findByEquipmentId(String equipmentId);

    List<StatusChangeLog> findByChangedAtAfter(java.time.LocalDateTime since);
}