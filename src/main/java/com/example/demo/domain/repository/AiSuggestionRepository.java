package com.example.demo.domain.repository;

import com.example.demo.domain.entity.AiSuggestion;
import com.example.demo.domain.enums.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, Long> {

    // 5.1 목록 조회용: PENDING + 만료 안 됨 (+ optional equipmentId)
    @Query("""
        SELECT s FROM AiSuggestion s
        WHERE s.status = com.example.demo.domain.enums.SuggestionStatus.PENDING
          AND s.validUntil > :now
          AND (:equipmentId IS NULL OR s.equipmentId = :equipmentId)
        ORDER BY s.generatedAt DESC
    """)
    Page<AiSuggestion> findActivePending(@Param("now") LocalDateTime now,
                                         @Param("equipmentId") String equipmentId,
                                         Pageable pageable);

    // 만료 일괄 처리용
    @Query("""
        SELECT s FROM AiSuggestion s
        WHERE s.status = com.example.demo.domain.enums.SuggestionStatus.PENDING
          AND s.validUntil <= :now
    """)
    List<AiSuggestion> findExpiredPending(@Param("now") LocalDateTime now);

    // 동일 파라미터 기존 PENDING 제안 (재생성 시 EXPIRED 처리용)
    List<AiSuggestion> findByEquipmentIdAndParameterTagAndStatus(
            String equipmentId, String parameterTag, SuggestionStatus status);
}
