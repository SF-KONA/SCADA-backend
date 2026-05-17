package com.example.demo.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class SuggestionDto {

    // ─── 5.1 목록 ─────────────────────────────────────
    @Getter @Builder
    public static class ListResponse {
        private Long total;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private List<ListItem> items;
    }

    @Getter @Builder
    public static class ListItem {
        private Long suggestionId;
        private String equipmentId;
        private String equipmentName;
        private String parameterTag;
        private String tagName;
        private String unit;
        private Double currentValue;
        private Double suggestedValue;
        private Double yieldImpact;
        private Double confidence;
        private Double currentOee;
        private Double predictedOee;
        private Double predictedAvailability;
        private Double predictedPerformance;
        private Double predictedQuality;
        private Double contributionScore;
        private String status;
        private LocalDateTime validUntil;
        private LocalDateTime generatedAt;
    }

    // ─── 5.2 상세 (5.1 필드 + normalMin/Max) ─────────────
    @Getter @Builder
    public static class DetailResponse {
        private Long suggestionId;
        private String equipmentId;
        private String equipmentName;
        private String parameterTag;
        private String tagName;
        private String unit;
        private Double normalMin;
        private Double normalMax;
        private Double currentValue;
        private Double suggestedValue;
        private Double yieldImpact;
        private Double confidence;
        private Double currentOee;
        private Double predictedOee;
        private Double predictedAvailability;
        private Double predictedPerformance;
        private Double predictedQuality;
        private Double contributionScore;
        private String status;
        private LocalDateTime validUntil;
        private LocalDateTime generatedAt;
    }

    // ─── 5.3 / 5.4 적용·거부 ───────────────────────────
    @Getter
    public static class ActionRequest {
        private String comment;
    }

    @Getter @Builder
    public static class ActionResponse {
        private Long actionId;
        private Long suggestionId;
        private String equipmentId;
        private String parameterTag;
        private Double beforeValue;
        private Double afterValue;
        private String actionType;
        private String userId;
        private String comment;
        private LocalDateTime actedAt;
    }

    // ─── 5.5 제어 가능 파라미터 목록 ─────────────────────
    @Getter @Builder
    public static class ControllableListResponse {
        private String equipmentId;
        private String equipmentName;
        private List<ControllableItem> items;
    }

    @Getter @Builder
    public static class ControllableItem {
        private Long paramId;
        private String tagCode;
        private String tagName;
        private String unit;
        private Double normalMin;
        private Double normalMax;
        private String dataType;
        private String paramCategory;
    }

    // ─── 5.6 파라미터 수동 변경 ─────────────────────────
    @Getter
    public static class ParamUpdateRequest {
        private Double newValue;
        private String comment;
    }

    @Getter @Builder
    public static class ParamUpdateResponse {
        private String equipmentId;
        private Long paramId;
        private String tagCode;
        private String tagName;
        private String unit;
        private Double beforeValue;
        private Double afterValue;
        private String userId;
        private String comment;
        private LocalDateTime changedAt;
    }
}
