package com.example.demo.domain.strategy;

import com.example.demo.domain.dto.ReportDto;
import com.example.demo.domain.dto.ReportQueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OEE_ANALYSIS - 설비별 OEE 추이 라인 차트
 * equipment_ids 1개: 단일 분석 / 2개 이상: 비교 분석
 */
@Component
@RequiredArgsConstructor
public class OeeAnalysisStrategy implements AnalysisStrategy {

    // TODO: OeeMetricsRepository 주입 (프로젝트 기존 레포 사용)
    // private final OeeMetricsRepository oeeRepo;

    @Override
    public String getType() { return "OEE_ANALYSIS"; }

    @Override
    public AnalysisResult analyze(ReportQueryParams params) {
        // DateRange range = DateRange.of(params.getPeriod());
        // List<OeeMetrics> metrics = oeeRepo.findByEquipmentIdsAndPeriod(
        //     params.getEquipmentIds(), range.getFrom(), range.getTo()
        // );

        // ── 임시 더미 (레포 연결 전) ─────────────────────────────────────────
        List<String> labels  = List.of("05-09", "05-10", "05-11", "05-12", "05-13");
        List<Double>  values = List.of(0.72, 0.75, 0.74, 0.73, 0.77);

        List<Map<String, Object>> datasets = params.getEquipmentIds().stream()
            .map(eqId -> {
                Map<String, Object> ds = new LinkedHashMap<>();
                ds.put("label", eqId);
                ds.put("data",  values);
                return ds;
            }).collect(Collectors.toList());

        ReportDto.ChartData chart = ReportDto.ChartData.builder()
            .type("line")
            .title(params.getEquipmentIds().size() > 1
                ? "설비 OEE 비교" : params.getEquipmentIds().get(0) + " OEE 추이")
            .data(Map.of("labels", labels, "datasets", datasets))
            .build();

        // rawMetrics (narrative 생성용)
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("equipment_ids", params.getEquipmentIds());
        raw.put("period",        params.getPeriod().getType());
        raw.put("avg_oee",       0.742);

        return new AnalysisResult(List.of(chart), List.of(), raw);
    }
}
