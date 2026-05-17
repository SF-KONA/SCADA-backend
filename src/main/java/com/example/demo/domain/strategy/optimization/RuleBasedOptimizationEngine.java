package com.example.demo.domain.strategy.optimization;

import com.example.demo.domain.entity.EquipmentParameter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 룰베이스 최적화 엔진.
 * 정상 범위 이탈을 감지하고 다음 규칙으로 suggested value / confidence / yieldImpact 산출:
 *
 *  - 이탈 폭이 작으면(한계값에서 rangeWidth × 10% 이내) 한계값에서 살짝(7.5%) 안쪽으로 추천
 *    → 공정 안정성 vs 생산성 손실 최소화
 *  - 이탈 폭이 크면 정상범위 중앙값 추천
 *  - confidence / yieldImpact 는 이탈 정도에 비례
 *  - contributionScore 는 제안 집합 내 정규화 이탈 점수 (0~1, 합=1)
 *
 * 추후 ML 기반 엔진(MlOptimizationEngine)으로 교체 가능하도록 OptimizationEngine 인터페이스만 노출.
 */
@Component
public class RuleBasedOptimizationEngine implements OptimizationEngine {

    private static final double SMALL_DEVIATION_THRESHOLD = 0.10; // 한계값 대비 rangeWidth × 10% 이내
    private static final double PULLBACK_RATIO            = 0.075; // 한계값에서 7.5% 안쪽
    private static final double DEFAULT_CURRENT_OEE       = 70.0;  // currentOee 미확보 시 fallback

    @Override
    public List<OptimizationProposal> propose(String equipmentId,
                                              List<ParameterSnapshot> snapshots,
                                              Double currentOee) {
        double baseOee = currentOee != null ? currentOee : DEFAULT_CURRENT_OEE;

        // 1차: 이탈 파라미터 추출 + rawDeviation 계산
        List<Draft> drafts = new ArrayList<>();
        for (ParameterSnapshot snap : snapshots) {
            Draft d = evaluate(snap);
            if (d != null) drafts.add(d);
        }
        if (drafts.isEmpty()) return List.of();

        // 2차: contributionScore 정규화 (각 raw / 합)
        double totalRaw = drafts.stream().mapToDouble(d -> d.rawContribution).sum();
        List<OptimizationProposal> result = new ArrayList<>(drafts.size());
        for (Draft d : drafts) {
            double contribution = totalRaw > 0 ? d.rawContribution / totalRaw : 0.0;
            double oeeDelta = clamp(d.yieldImpact * 10.0, 0.0, 100.0 - baseOee);
            double predicted = clamp(baseOee + oeeDelta, 0.0, 100.0);

            // OEE 변화량을 가용률/성능률/품질률에 가중치 분배
            double predAvailability = round1(oeeDelta * 0.20);
            double predPerformance  = round1(oeeDelta * 0.40);
            double predQuality      = round1(oeeDelta * 0.40);

            result.add(new OptimizationProposal(
                    d.paramId,
                    d.parameterTag,
                    d.currentValue,
                    round2(d.suggestedValue),
                    round2(d.confidence),
                    round2(d.yieldImpact),
                    round1(baseOee),
                    round1(predicted),
                    predAvailability,
                    predPerformance,
                    predQuality,
                    round2(contribution)
            ));
        }
        return result;
    }

    /** 단일 파라미터 평가. 정상 범위 내거나 평가 불가 시 null. */
    private Draft evaluate(ParameterSnapshot snap) {
        EquipmentParameter p = snap.parameter();
        Double value = snap.latestValue();
        if (value == null || p.getNormalMin() == null || p.getNormalMax() == null) return null;

        double min = p.getNormalMin();
        double max = p.getNormalMax();
        double rangeWidth = max - min;
        if (rangeWidth <= 0) return null;

        // 정상 범위 내면 제안 없음
        if (value >= min && value <= max) return null;

        boolean overMax = value > max;
        double limit = overMax ? max : min;
        double excess = Math.abs(value - limit);              // 한계값으로부터 이탈 거리
        double excessRatio = excess / rangeWidth;             // 0+ ~ 이론상 무한
        double midpoint = (min + max) / 2.0;

        // suggested value
        double suggested;
        if (excessRatio <= SMALL_DEVIATION_THRESHOLD) {
            // 작게 벗어남 → 한계값에서 7.5% 안쪽
            suggested = overMax
                    ? max - PULLBACK_RATIO * rangeWidth
                    : min + PULLBACK_RATIO * rangeWidth;
        } else {
            // 크게 벗어남 → 중앙값
            suggested = midpoint;
        }

        // confidence: 0.7 ~ 0.95, 이탈 폭이 클수록 높음
        double normalizedExcess = Math.min(excessRatio, 1.0);
        double confidence = 0.70 + normalizedExcess * 0.25;

        // yieldImpact: 0.3 ~ 0.9
        double yieldImpact = 0.30 + normalizedExcess * 0.60;

        // 정규화 이탈 (contributionScore 분자): |현재값 - 중앙값| / rangeWidth
        double rawContribution = Math.abs(value - midpoint) / rangeWidth;

        return new Draft(p.getParamId(), p.getTagCode(), value, suggested,
                confidence, yieldImpact, rawContribution);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /** 1차 평가 결과 (contribution 정규화 전). */
    private record Draft(
            Long paramId,
            String parameterTag,
            Double currentValue,
            Double suggestedValue,
            Double confidence,
            Double yieldImpact,
            Double rawContribution
    ) {
    }
}
