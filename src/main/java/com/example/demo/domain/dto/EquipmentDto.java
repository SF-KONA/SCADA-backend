package com.example.demo.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class EquipmentDto {

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

    @Getter
    public static class NoteRequest {
        private String noteText;
    }

    // 수정 요청 DTO 추가
    @Getter
    public static class NoteUpdateRequest {
        private String noteText;
    }

    @Getter @Builder
    public static class NoteResponse {
        private Long noteId;
        private String userId;
        private String equipmentId;
        private String noteText;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt; // 추가
    }

    @Getter @Builder
    public static class NoteListResponse {
        private String equipmentId;
        private List<NoteResponse> items;
    }
}