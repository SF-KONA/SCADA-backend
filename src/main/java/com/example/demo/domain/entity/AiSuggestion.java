package com.example.demo.domain.entity;
import com.example.demo.domain.enums.SuggestionStatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suggestion_id")
    private Long suggestionId;

    @Column(name = "equipment_id", nullable = false, length = 16)
    private String equipmentId;

    @Column(name = "parameter_tag", nullable = false, length = 64)
    private String parameterTag;  // 제어 대상 태그 (예: FURN_01_TEMP)

    @Column(name = "current_value")
    private Double currentValue;  // 제안 생성 시점 현재값

    @Column(name = "suggested_value")
    private Double suggestedValue;  // AI 제안값

    @Column(name = "yield_impact")
    private Double yieldImpact;  // 예상 수율 영향도 (0~1)

    @Column
    private Double confidence;  // 0~1 모델 신뢰도

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;  // 만료 시각

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "current_oee")
    private Double currentOee;

    @Column(name = "predicted_oee")
    private Double predictedOee;

    @Column(name = "predicted_availability")
    private Double predictedAvailability;  // 가용률 변화량 %p

    @Column(name = "predicted_quality")
    private Double predictedQuality;  // 품질률 변화량 %p

    @Column(name = "predicted_performance")
    private Double predictedPerformance;  // 성능률 변화량 %p

    @Column(name = "contribution_score")
    private Double contributionScore;  // 정규화된 이탈 기여도 (0~1). 추후 SHAP 등으로 교체 예정
}
