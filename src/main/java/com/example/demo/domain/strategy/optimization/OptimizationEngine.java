package com.example.demo.domain.strategy.optimization;

import com.example.demo.domain.entity.EquipmentParameter;

import java.util.List;

/**
 * 공정 파라미터 최적화 제안 엔진.
 * 현재는 RuleBasedOptimizationEngine 단일 구현. 추후 ML 엔진으로 교체 가능.
 */
public interface OptimizationEngine {

    /**
     * 설비의 제어 가능 파라미터 스냅샷을 입력받아 최적화 제안 목록을 반환.
     * 정상 범위 내 파라미터는 제외된다.
     */
    List<OptimizationProposal> propose(String equipmentId,
                                       List<ParameterSnapshot> snapshots,
                                       Double currentOee);

    /** 엔진이 평가할 단일 파라미터의 스냅샷 (파라미터 정의 + 최신 측정값). */
    record ParameterSnapshot(EquipmentParameter parameter, Double latestValue) {
    }
}
