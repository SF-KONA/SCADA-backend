package com.example.demo.domain.strategy.optimization;

/**
 * 최적화 엔진이 단일 파라미터에 대해 산출한 제안값.
 * 엔진 구현체(RuleBased / ML)와 무관하게 동일한 형식으로 전달된다.
 */
public record OptimizationProposal(
        Long paramId,
        String parameterTag,
        Double currentValue,
        Double suggestedValue,
        Double confidence,             // 0~1
        Double yieldImpact,            // 0~1
        Double currentOee,             // 0~100 (%)
        Double predictedOee,           // 0~100 (%)
        Double predictedAvailability,  // %p (변화량)
        Double predictedPerformance,   // %p
        Double predictedQuality,       // %p
        Double contributionScore       // 0~1 (정규화된 이탈 기여도)
) {
}
