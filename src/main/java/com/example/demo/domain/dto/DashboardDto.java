package com.example.demo.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

public class DashboardDto {

    // ── GET /api/dashboard/kpi ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class StatusResponse {
        private int total;
        private int running;
        private int warning;
        private int alarm;
        private int maintenance;
        private int idle;
        private OffsetDateTime updatedAt;
    }

    // ── GET /api/dashboard/oee ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class OeeResponse {
        private String period;
        private Double oee;
        private Double target;
        private Double prevOee;
        private Double availability;
        private Double performance;
        private Double quality;
        private List<OeeTrendPoint> trend;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class OeeTrendPoint {
        private OffsetDateTime periodStart;
        private Double oee;
    }

    // ── GET /api/dashboard/defect-rate ────────────────────────────────────
    @Getter
    @Builder
    public static class DefectRateResponse {
        private String period;
        private List<ProcessDefect> processes;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class ProcessDefect {
        private String stepNo;
        private String processName;
        private Double defectRate;
        private String alertLevel;  // NORMAL / WARNING / CRITICAL
    }

    // ── GET /api/dashboard/yield ──────────────────────────────────────────
    @Getter
    @Builder
    public static class YieldResponse {
        private String period;
        private Double yield;
        private Double target;
        private Double prevYield;
        private List<YieldTrendPoint> trend;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class YieldTrendPoint {
        private OffsetDateTime periodStart;
        private Double yield;
    }

    // ── GET /api/dashboard/equipments ────────────────────────────────────
    @Getter
    @Builder
    public static class EquipmentsResponse {
        private int total;
        private List<EquipmentItem> equipments;  // 명세: result.equipments
    }

    @Getter
    @Builder
    public static class EquipmentItem {
        private String equipmentId;
        private String equipmentName;
        private String stepNo;
        private String processName;
        private String status;       // IDLE / RUN / ALARM
        private String statusLabel;
        private Double oee;
        private AlarmSummary lastAlarm;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class AlarmSummary {
        private Long alarmId;
        private String sourceType;
        private String severity;
        private String message;
        private OffsetDateTime occurredAt;
    }
}
