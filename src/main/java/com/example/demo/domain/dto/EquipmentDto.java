package com.example.demo.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class EquipmentDto {

    // ─── 4.1 공정별 설비 목록 ──────────────────
    @Getter @Builder
    public static class EquipmentListResponse {
        private String stepNo;
        private String processName;
        private List<EquipmentItem> items;
    }

    @Getter @Builder
    public static class EquipmentItem {
        private String equipmentId;
        private String equipmentName;
        private Integer unitNo;
        private Integer currentStatus;
        private String currentStatusLabel;
        private Integer totalRunningHours;
    }

    // ─── 4.2 설비 파라미터·측정값 ──────────────
    @Getter @Builder
    public static class ParameterListResponse {
        private String equipmentId;
        private String equipmentName;
        private Integer currentStatus;
        private String currentStatusLabel;
        private String overallStatus;
        private String overallStatusLabel;
        private List<ParameterItem> parameters;
    }

    @Getter @Builder
    public static class ParameterItem {
        private Long paramId;
        private String tagCode;
        private String tagName;
        private String unit;
        private Double normalMin;
        private Double normalMax;
        private Double latestValue;
        private LocalDateTime latestAt;
        private String paramStatus;
        private List<MeasurementItem> history;
    }

    @Getter @Builder
    public static class MeasurementItem {
        private Double value;
        private LocalDateTime measuredAt;
    }

    // ─── 4.3 설비 알람 ────────────────────────
    @Getter @Builder
    public static class AlarmListResponse {
        private String equipmentId;
        private List<AlarmItem> items;
    }

    @Getter @Builder
    public static class AlarmItem {
        private Long alarmId;
        private String severity;
        private String tagName;
        private String message;
        private Double triggeredValue;
        private Integer occurrenceCount;
        private String status;
        private String statusLabel;
        private LocalDateTime occurredAt;
    }

    // ─── 4.4 설비 이벤트 로그 ─────────────────
    @Getter @Builder
    public static class EventListResponse {
        private String equipmentId;
        private Long total;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private List<EventItem> items;
    }

    @Getter @Builder
    public static class EventItem {
        private String eventType;
        private String eventLabel;
        private String message;
        private String severity;
        private String severityLabel;
        private LocalDateTime occurredAt;
    }

    // ─── 4.5 관리자 의견 추가 ─────────────────
    @Getter
    public static class NoteRequest {
        private String noteText;
    }

    @Getter @Builder
    public static class NoteResponse {
        private Long noteId;
        private String userId;
        private String equipmentId;
        private String noteText;
        private LocalDateTime createdAt;
    }

    // ─── 4.6 관리자 의견 목록 조회 (추가) ────────
    @Getter @Builder
    public static class NoteListResponse {
        private String equipmentId;
        private List<NoteResponse> items;
    }
}