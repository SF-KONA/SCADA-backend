package com.example.demo.domain.dto;

import com.example.demo.domain.enums.AlarmSeverity;
import com.example.demo.domain.enums.AlarmSourceType;
import com.example.demo.domain.enums.AlarmStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

public class AlarmDto {

    // ─────────────────────────────────────────────
    // 목록 아이템 (2.1)
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ListItem {
        private Long alarmId;
        private AlarmSourceType sourceType;
        private String equipmentId;
        private String equipmentName;
        private String stepNo;
        private String processName;
        private String zoneCode;
        private AlarmSeverity severity;
        private String message;
        private AlarmStatus status;
        private OffsetDateTime occurredAt;
        private OffsetDateTime lastOccurredAt;
        private Integer occurrenceCount;
        private String ackUserId;
        private OffsetDateTime ackAt;
        private String startUserId;
        private OffsetDateTime startAt;
        private String doneUserId;
        private OffsetDateTime doneAt;
    }

    // ─────────────────────────────────────────────
    // 페이지 응답 래퍼
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class PageResponse {
        private long total;
        private int page;
        private int size;
        private int totalPages;
        private List<ListItem> items;
        private long totalErr;
        private long totalWarn;
        private long totalInfo;
    }

    // ─────────────────────────────────────────────
    // 상세 응답 (2.2)
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Detail {
        private Long alarmId;
        private AlarmSourceType sourceType;
        private String equipmentId;
        private String equipmentName;
        private String stepNo;
        private String processName;
        private Long paramId;
        private String tagCode;
        private String tagName;
        private String unit;
        private Double normalMin;
        private Double normalMax;
        private String zoneCode;
        private AlarmSeverity severity;
        private String message;
        private AlarmStatus status;
        private OffsetDateTime occurredAt;
        private OffsetDateTime lastOccurredAt;
        private Integer occurrenceCount;
        private Double triggeredValue;
        private String ackUserId;
        private OffsetDateTime ackAt;
        private String startUserId;
        private OffsetDateTime startAt;
        private String doneUserId;
        private OffsetDateTime doneAt;
        private String doneComment;
    }

    // ─────────────────────────────────────────────
    // ACK 응답 (2.3)
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class AckResponse {
        private Long alarmId;
        private AlarmStatus status;
        private String ackUserId;
        private OffsetDateTime ackAt;
    }

    // ─────────────────────────────────────────────
    // 처리 시작 응답 (2.4)
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class StartResponse {
        private Long alarmId;
        private AlarmStatus status;
        private String startUserId;
        private OffsetDateTime startAt;
    }

    // ─────────────────────────────────────────────
    // 처리 완료 응답 (2.5)
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class DoneResponse {
        private Long alarmId;
        private AlarmStatus status;
        private String doneUserId;
        private OffsetDateTime doneAt;
        private String doneComment;
    }

    // ─────────────────────────────────────────────
    // 처리 완료 요청 바디 (2.5)
    // ─────────────────────────────────────────────
    @Getter
    public static class DoneRequest {
        @Size(max = 500, message = "comment는 500자를 초과할 수 없습니다.")
        private String comment;
    }

    // ─────────────────────────────────────────────
    // 비상 알람 아이템 (2.6)
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmergencyItem {
        private Long alarmId;
        private AlarmSourceType sourceType;
        private String equipmentId;
        private String equipmentName;
        private String stepNo;
        private String processName;
        private String zoneCode;
        private String tagCode;
        private String tagName;
        private String unit;
        private Double triggeredValue;
        private Double normalMin;
        private Double normalMax;
        private AlarmSeverity severity;
        private String message;
        private AlarmStatus status;
        private OffsetDateTime occurredAt;
        private Integer occurrenceCount;
    }

    // ─────────────────────────────────────────────
    // 비상 알람 응답 래퍼 (2.6)
    // ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class EmergencyResponse {
        private int count;
        private List<EmergencyItem> items;
    }
}