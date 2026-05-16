package com.example.demo.domain.strategy;

import com.example.demo.domain.dto.ReportDto;
import com.example.demo.domain.dto.ReportQueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ALARM_FREQUENCY - 알람 빈도 바 차트 + severity 파이 차트 + FMEA
 */
@Component
@RequiredArgsConstructor
public class AlarmFrequencyStrategy implements AnalysisStrategy {

    // TODO: AlarmRepository 주입
    // private final AlarmRepository alarmRepo;

    @Override
    public String getType() { return "ALARM_FREQUENCY"; }

    @Override
    public AnalysisResult analyze(ReportQueryParams params) {
        // ── 임시 더미 데이터 ─────────────────────────────────────────────────
        List<String> equipIds = params.getEquipmentIds().isEmpty()
            ? List.of("FURN_01", "FURN_02") : params.getEquipmentIds();

        // 바 차트
        ReportDto.ChartData barChart = ReportDto.ChartData.builder()
            .type("bar").title("알람 빈도")
            .data(Map.of(
                "labels",   equipIds,
                "datasets", List.of(Map.of("label", "알람 횟수", "data", List.of(12, 7)))
            )).build();

        // 파이 차트
        ReportDto.ChartData pieChart = ReportDto.ChartData.builder()
            .type("pie").title("심각도 비율")
            .data(Map.of(
                "labels",   List.of("ERR", "WARN", "INFO"),
                "datasets", List.of(Map.of("data", List.of(3, 8, 8)))
            )).build();

        // FMEA (설비별 대표 알람 기준)
        List<ReportDto.FmeaEntry> fmea = List.of(
            FmeaCalculator.build(
                equipIds.get(0) + " 반복 알람",
                "생산 중단 위험 / 품질 저하",
                "설비 이상 또는 파라미터 임계 초과",
                "ERR", 12, 2.5
            )
        );

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("total_alarms", 19);
        raw.put("by_severity",  Map.of("ERR", 3, "WARN", 8, "INFO", 8));

        return new AnalysisResult(List.of(barChart, pieChart), fmea, raw);
    }
}
