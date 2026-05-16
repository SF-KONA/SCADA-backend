package com.example.demo.domain.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

public class ProcessDto {

    @Getter @Builder
    public static class ProcessCardItem {
        private String stepNo;
        private String processName;
        private Integer sortOrder;
        private Boolean hasEquipment;
        private String equipmentType;
        private Integer totalEquipment;
        private Integer runningEquipment;
        private Double utilizationRate;
        private String status;
        private String statusLabel;
        private List<AlertItem> alerts;
        private LocalDateTime lastUpdatedAt;
    }

    @Getter @Builder
    public static class AlertItem {
        private Long alarmId;
        private String sourceType;
        private String equipmentId;
        private String equipmentName;
        private String message;
        private String severity;
        private Double triggeredValue;
        private LocalDateTime occurredAt;
    }

    @Getter @Builder
    public static class ProcessListResponse {
        private LocalDateTime updatedAt;
        private List<ProcessCardItem> items;
    }

    @Getter @Builder
    public static class PartnerResponse {
        private String stepNo;
        private String processName;
        private PartnerInfo partner;
        private InventoryInfo inventory;
        private DeliveryInfo delivery;
        private LocalDateTime updatedAt;
    }

    @Getter @Builder
    public static class PartnerInfo {
        private String companyName;
        private String productName;
        private String managerName;
        private String contactTel;
        private String contactEmail;
    }

    @Getter @Builder
    public static class InventoryInfo {
        private Integer stockQty;
        private String stockUnit;
        private Integer safetyStock;
        private String stockStatus;
        private String stockStatusLabel;
    }

    @Getter @Builder
    public static class DeliveryInfo {
        private String deadline;
        private String deliveryStatus;
        private String deliveryStatusLabel;
        private String delayReason;
    }
}