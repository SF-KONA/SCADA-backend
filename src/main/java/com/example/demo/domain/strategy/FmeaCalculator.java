package com.example.demo.domain.strategy;

import com.example.demo.domain.dto.ReportDto;

/**
 * FMEA SOD 점수 계산 유틸 (확정 기준)
 *
 * S (심각도): ERR=5 / WARN=3 / INFO=1
 * O (발생도): 7이상=7 / 5이상=5 / 3이상=3 / 3미만=1
 * D (검출도): 1시간이내=1 / 하루이내=3 / 하루초과=7
 * RPN = S × O × D
 * RPN 70이상=HIGH / 30~69=MEDIUM / 30미만=LOW
 */
public class FmeaCalculator {

    public static int calcS(String severity) {
        return switch (severity) {
            case "ERR"  -> 5;
            case "WARN" -> 3;
            default     -> 1;
        };
    }

    public static int calcO(int occurrenceCount) {
        if (occurrenceCount >= 7) return 7;
        if (occurrenceCount >= 5) return 5;
        if (occurrenceCount >= 3) return 3;
        return 1;
    }

    public static int calcD(double avgResolutionHours) {
        if (avgResolutionHours <= 1)  return 1;
        if (avgResolutionHours <= 24) return 3;
        return 7;
    }

    public static String riskLevel(int rpn) {
        if (rpn >= 70) return "HIGH";
        if (rpn >= 30) return "MEDIUM";
        return "LOW";
    }

    public static String recommendAction(int rpn) {
        if (rpn >= 70) return "즉시 점검 및 원인 분석 필요";
        if (rpn >= 30) return "모니터링 강화 및 주기적 점검";
        return "정상 범위, 현행 유지";
    }

    public static ReportDto.FmeaEntry build(
            String failureMode, String effect, String cause,
            String severity, int count, double avgHours) {

        int s   = calcS(severity);
        int o   = calcO(count);
        int d   = calcD(avgHours);
        int rpn = s * o * d;

        return ReportDto.FmeaEntry.builder()
            .failureMode(failureMode)
            .effect(effect)
            .cause(cause)
            .s(s).o(o).d(d).rpn(rpn)
            .action(recommendAction(rpn))
            .build();
    }
}
