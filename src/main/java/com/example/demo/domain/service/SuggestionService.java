package com.example.demo.domain.service;

import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.SuggestionDto;
import com.example.demo.domain.entity.AiSuggestion;
import com.example.demo.domain.entity.Equipment;
import com.example.demo.domain.entity.EquipmentMeasurement;
import com.example.demo.domain.entity.EquipmentParameter;
import com.example.demo.domain.entity.OeeMetric;
import com.example.demo.domain.entity.OptimizationAction;
import com.example.demo.domain.enums.ActionType;
import com.example.demo.domain.enums.SuggestionStatus;
import com.example.demo.domain.repository.AiSuggestionRepository;
import com.example.demo.domain.repository.EquipmentMeasurementRepository;
import com.example.demo.domain.repository.EquipmentParameterRepository;
import com.example.demo.domain.repository.EquipmentRepository;
import com.example.demo.domain.repository.OeeMetricRepository;
import com.example.demo.domain.repository.OptimizationActionRepository;
import com.example.demo.domain.strategy.optimization.OptimizationEngine;
import com.example.demo.domain.strategy.optimization.OptimizationProposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {

    private static final int    SUGGESTION_VALID_MINUTES = 30;
    private static final double MIN_CONFIDENCE           = 0.70;
    private static final double MIN_OEE_DELTA_PCT_POINT  = 1.0;
    private static final double VALUE_EQUALITY_EPSILON   = 0.01; // 측정값 동일 판단 허용 오차

    private final AiSuggestionRepository       suggestionRepo;
    private final OptimizationActionRepository actionRepo;
    private final EquipmentRepository          equipmentRepo;
    private final EquipmentParameterRepository parameterRepo;
    private final EquipmentMeasurementRepository measurementRepo;
    private final OeeMetricRepository          oeeMetricRepo;
    private final OptimizationEngine           optimizationEngine;

    // ─── 5.1 목록 ─────────────────────────────────────
    @Transactional
    public SuggestionDto.ListResponse list(int page, int size, String equipmentId) {
        LocalDateTime now = LocalDateTime.now();
        expireOverdue(now); // 만료된 PENDING 일괄 EXPIRED 처리

        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<AiSuggestion> result = suggestionRepo.findActivePending(now, equipmentId, pageable);

        // 표시용 메타데이터(equipmentName, tagName, unit) 일괄 조회
        Map<String, Equipment> equipmentMap = lookupEquipments(result.getContent());
        Map<ParamKey, EquipmentParameter> paramMap = lookupParameters(result.getContent());

        List<SuggestionDto.ListItem> items = result.getContent().stream()
                .map(s -> toListItem(s, equipmentMap, paramMap))
                .toList();

        return SuggestionDto.ListResponse.builder()
                .total(result.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(result.getTotalPages())
                .items(items)
                .build();
    }

    // ─── 5.2 상세 ─────────────────────────────────────
    @Transactional(readOnly = true)
    public SuggestionDto.DetailResponse get(Long suggestionId) {
        AiSuggestion s = suggestionRepo.findById(suggestionId)
                .orElseThrow(() -> new ReportException(ErrorCode.SUGGESTION_NOT_FOUND));

        Equipment eq = equipmentRepo.findById(s.getEquipmentId()).orElse(null);
        EquipmentParameter param = parameterRepo.findByEquipmentId(s.getEquipmentId()).stream()
                .filter(p -> p.getTagCode().equals(s.getParameterTag()))
                .findFirst().orElse(null);

        return SuggestionDto.DetailResponse.builder()
                .suggestionId(s.getSuggestionId())
                .equipmentId(s.getEquipmentId())
                .equipmentName(eq != null ? eq.getEquipmentName() : null)
                .parameterTag(s.getParameterTag())
                .tagName(param != null ? param.getTagName() : null)
                .unit(param != null ? param.getUnit() : null)
                .normalMin(param != null ? param.getNormalMin() : null)
                .normalMax(param != null ? param.getNormalMax() : null)
                .currentValue(s.getCurrentValue())
                .suggestedValue(s.getSuggestedValue())
                .yieldImpact(s.getYieldImpact())
                .confidence(s.getConfidence())
                .currentOee(s.getCurrentOee())
                .predictedOee(s.getPredictedOee())
                .predictedAvailability(s.getPredictedAvailability())
                .predictedPerformance(s.getPredictedPerformance())
                .predictedQuality(s.getPredictedQuality())
                .contributionScore(s.getContributionScore())
                .status(s.getStatus().name())
                .validUntil(s.getValidUntil())
                .generatedAt(s.getGeneratedAt())
                .build();
    }

    // ─── 5.3 적용 ─────────────────────────────────────
    @Transactional
    public SuggestionDto.ActionResponse apply(Long suggestionId, String userId, String comment) {
        validateComment(comment);
        AiSuggestion s = suggestionRepo.findById(suggestionId)
                .orElseThrow(() -> new ReportException(ErrorCode.SUGGESTION_NOT_FOUND));

        ensurePending(s);

        s.setStatus(SuggestionStatus.APPLIED);
        suggestionRepo.save(s);

        OptimizationAction action = OptimizationAction.builder()
                .suggestionId(s.getSuggestionId())
                .equipmentId(s.getEquipmentId())
                .parameterTag(s.getParameterTag())
                .beforeValue(s.getCurrentValue())
                .afterValue(s.getSuggestedValue())
                .actionType(ActionType.APPLY)
                .userId(userId)
                .comment(comment)
                .actedAt(LocalDateTime.now())
                .build();
        OptimizationAction saved = actionRepo.save(action);

        return toActionResponse(saved);
    }

    // ─── 5.4 거부 ─────────────────────────────────────
    @Transactional
    public SuggestionDto.ActionResponse reject(Long suggestionId, String userId, String comment) {
        validateComment(comment);
        AiSuggestion s = suggestionRepo.findById(suggestionId)
                .orElseThrow(() -> new ReportException(ErrorCode.SUGGESTION_NOT_FOUND));

        ensurePending(s);

        s.setStatus(SuggestionStatus.REJECTED);
        suggestionRepo.save(s);

        OptimizationAction action = OptimizationAction.builder()
                .suggestionId(s.getSuggestionId())
                .equipmentId(s.getEquipmentId())
                .parameterTag(s.getParameterTag())
                .beforeValue(s.getCurrentValue())
                .afterValue(s.getCurrentValue()) // 거부 시 before=after
                .actionType(ActionType.REJECT)
                .userId(userId)
                .comment(comment)
                .actedAt(LocalDateTime.now())
                .build();
        OptimizationAction saved = actionRepo.save(action);

        return toActionResponse(saved);
    }

    // ─── 제안 생성 (스케줄러 / 수동 트리거 공용) ──────────
    /**
     * 단일 설비에 대해 룰베이스 엔진 평가 후 신뢰도·OEE 개선 필터를 통과한 제안만 INSERT.
     * 동일 파라미터에 PENDING 제안이 있으면 EXPIRED 처리 후 새로 INSERT.
     *
     * @return 새로 생성된 제안 수
     */
    @Transactional
    public int generateForEquipment(String equipmentId) {
        Equipment equipment = equipmentRepo.findById(equipmentId).orElse(null);
        if (equipment == null) return 0;

        // 1) 제어 가능 파라미터 + 최신 측정값 스냅샷
        List<EquipmentParameter> controllable = parameterRepo.findByEquipmentId(equipmentId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsControllable()))
                .toList();
        if (controllable.isEmpty()) return 0;

        List<OptimizationEngine.ParameterSnapshot> snapshots = controllable.stream()
                .map(p -> new OptimizationEngine.ParameterSnapshot(
                        p,
                        measurementRepo.findLatestByParamId(p.getParamId())
                                .map(EquipmentMeasurement::getMeasuredValue)
                                .orElse(null)))
                .toList();

        // 2) 현재 OEE
        Double currentOee = oeeMetricRepo.findLatestByEquipmentId(equipmentId)
                .map(OeeMetric::getOee)
                .orElse(null);

        // 3) 엔진 호출
        List<OptimizationProposal> proposals =
                optimizationEngine.propose(equipmentId, snapshots, currentOee);

        // 4) 필터 + 영속화
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = now.plusMinutes(SUGGESTION_VALID_MINUTES);
        int created = 0;

        for (OptimizationProposal proposal : proposals) {
            if (!passesFilter(proposal)) continue;

            // 동일 파라미터의 살아있는 PENDING 조회
            List<AiSuggestion> existingPending = suggestionRepo
                    .findByEquipmentIdAndParameterTagAndStatus(
                            equipmentId, proposal.parameterTag(), SuggestionStatus.PENDING);

            // 측정값이 그대로면(±0.01 이내) 기존 PENDING 유지: 신규 INSERT 안 함
            // → 실 SCADA 동작에 부합, 시연 시 suggestionId 안정성 확보
            boolean valueUnchanged = existingPending.stream().anyMatch(old ->
                    old.getValidUntil() != null
                            && old.getValidUntil().isAfter(now)
                            && approxEqual(old.getCurrentValue(), proposal.currentValue()));
            if (valueUnchanged) continue;

            // 측정값이 변경됨 → 기존 PENDING은 EXPIRED 처리 후 새로 INSERT
            for (AiSuggestion old : existingPending) {
                old.setStatus(SuggestionStatus.EXPIRED);
            }
            if (!existingPending.isEmpty()) suggestionRepo.saveAll(existingPending);

            AiSuggestion s = AiSuggestion.builder()
                    .equipmentId(equipmentId)
                    .parameterTag(proposal.parameterTag())
                    .currentValue(proposal.currentValue())
                    .suggestedValue(proposal.suggestedValue())
                    .yieldImpact(proposal.yieldImpact())
                    .confidence(proposal.confidence())
                    .currentOee(proposal.currentOee())
                    .predictedOee(proposal.predictedOee())
                    .predictedAvailability(proposal.predictedAvailability())
                    .predictedPerformance(proposal.predictedPerformance())
                    .predictedQuality(proposal.predictedQuality())
                    .contributionScore(proposal.contributionScore())
                    .status(SuggestionStatus.PENDING)
                    .validUntil(validUntil)
                    .generatedAt(now)
                    .build();
            suggestionRepo.save(s);
            created++;
        }
        return created;
    }

    // ─── 최적화 적용/거부 이력 ───────────────────────
    @Transactional(readOnly = true)
    public List<SuggestionDto.HistoryItem> getHistory(String equipmentId, int size) {
        if (equipmentId == null || equipmentId.isBlank()) {
            throw new ReportException(ErrorCode.BAD_REQUEST);
        }
        PageRequest pageable = PageRequest.of(0, Math.max(1, Math.min(size, 100)));
        return actionRepo.findByEquipmentIdOrderByActedAtDesc(equipmentId, pageable).stream()
                .map(a -> SuggestionDto.HistoryItem.builder()
                        .actionId(a.getActionId())
                        .equipmentId(a.getEquipmentId())
                        .parameterTag(a.getParameterTag())
                        .actionType(a.getActionType().name())
                        .beforeValue(a.getBeforeValue())
                        .afterValue(a.getAfterValue())
                        .comment(a.getComment())
                        .actedAt(a.getActedAt())
                        .build())
                .toList();
    }

    /** 전체 설비를 순회하며 제안 생성. 스케줄러 / 수동 일괄 트리거가 호출. */
    @Transactional
    public int generateForAll() {
        int total = 0;
        for (Equipment eq : equipmentRepo.findAll()) {
            total += generateForEquipment(eq.getEquipmentId());
        }
        return total;
    }

    // ─── helpers ─────────────────────────────────────
    private static boolean approxEqual(Double a, Double b) {
        if (a == null || b == null) return false;
        return Math.abs(a - b) <= VALUE_EQUALITY_EPSILON;
    }

    private boolean passesFilter(OptimizationProposal p) {
        if (p.confidence() == null || p.confidence() < MIN_CONFIDENCE) return false;
        if (p.currentOee() == null || p.predictedOee() == null) return false;
        return (p.predictedOee() - p.currentOee()) >= MIN_OEE_DELTA_PCT_POINT;
    }

    private void expireOverdue(LocalDateTime now) {
        List<AiSuggestion> overdue = suggestionRepo.findExpiredPending(now);
        if (overdue.isEmpty()) return;
        for (AiSuggestion s : overdue) s.setStatus(SuggestionStatus.EXPIRED);
        suggestionRepo.saveAll(overdue);
    }

    private void ensurePending(AiSuggestion s) {
        if (s.getValidUntil() != null && s.getValidUntil().isBefore(LocalDateTime.now())) {
            s.setStatus(SuggestionStatus.EXPIRED);
            suggestionRepo.save(s);
            throw new ReportException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (s.getStatus() != SuggestionStatus.PENDING) {
            throw new ReportException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void validateComment(String comment) {
        if (comment != null && comment.length() > 200) {
            throw new ReportException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private Map<String, Equipment> lookupEquipments(List<AiSuggestion> items) {
        List<String> ids = items.stream().map(AiSuggestion::getEquipmentId).distinct().toList();
        return equipmentRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, Function.identity()));
    }

    private record ParamKey(String equipmentId, String tagCode) {}

    private Map<ParamKey, EquipmentParameter> lookupParameters(List<AiSuggestion> items) {
        // suggestion 수가 많지 않으므로 equipment 단위로 모아서 한 번씩 조회
        List<String> equipmentIds = items.stream().map(AiSuggestion::getEquipmentId).distinct().toList();
        Map<ParamKey, EquipmentParameter> map = new java.util.HashMap<>();
        for (String eqId : equipmentIds) {
            for (EquipmentParameter p : parameterRepo.findByEquipmentId(eqId)) {
                map.put(new ParamKey(eqId, p.getTagCode()), p);
            }
        }
        return map;
    }

    private SuggestionDto.ListItem toListItem(AiSuggestion s,
                                              Map<String, Equipment> equipmentMap,
                                              Map<ParamKey, EquipmentParameter> paramMap) {
        Equipment eq = equipmentMap.get(s.getEquipmentId());
        EquipmentParameter param = paramMap.get(new ParamKey(s.getEquipmentId(), s.getParameterTag()));
        return SuggestionDto.ListItem.builder()
                .suggestionId(s.getSuggestionId())
                .equipmentId(s.getEquipmentId())
                .equipmentName(eq != null ? eq.getEquipmentName() : null)
                .parameterTag(s.getParameterTag())
                .tagName(param != null ? param.getTagName() : null)
                .unit(param != null ? param.getUnit() : null)
                .currentValue(s.getCurrentValue())
                .suggestedValue(s.getSuggestedValue())
                .yieldImpact(s.getYieldImpact())
                .confidence(s.getConfidence())
                .currentOee(s.getCurrentOee())
                .predictedOee(s.getPredictedOee())
                .predictedAvailability(s.getPredictedAvailability())
                .predictedPerformance(s.getPredictedPerformance())
                .predictedQuality(s.getPredictedQuality())
                .contributionScore(s.getContributionScore())
                .status(s.getStatus().name())
                .validUntil(s.getValidUntil())
                .generatedAt(s.getGeneratedAt())
                .build();
    }

    private SuggestionDto.ActionResponse toActionResponse(OptimizationAction a) {
        return SuggestionDto.ActionResponse.builder()
                .actionId(a.getActionId())
                .suggestionId(a.getSuggestionId())
                .equipmentId(a.getEquipmentId())
                .parameterTag(a.getParameterTag())
                .beforeValue(a.getBeforeValue())
                .afterValue(a.getAfterValue())
                .actionType(a.getActionType().name())
                .userId(a.getUserId())
                .comment(a.getComment())
                .actedAt(a.getActedAt())
                .build();
    }
}
