package com.example.demo.domain.service;

import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.ProcessDto;
import com.example.demo.domain.entity.*;
import com.example.demo.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessRepository processRepository;
    private final EquipmentRepository equipmentRepository;
    private final AlarmRepository alarmRepository;
    private final PartnerRepository partnerRepository;

    @Transactional(readOnly = true)
    public ProcessDto.ProcessListResponse getProcessList() {

        List<ProcessEntity> processes = processRepository.findAllByOrderBySortOrderAsc();
        List<ProcessDto.ProcessCardItem> items = new ArrayList<>();

        for (ProcessEntity process : processes) {
            List<ProcessDto.AlertItem> alerts = new ArrayList<>();
            String status = "NORMAL";
            LocalDateTime lastUpdatedAt = null;

            if (process.getHasEquipment()) {
                List<Equipment> equipments = equipmentRepository.findByStepNo(process.getStepNo());
                int total = equipments.size();
                int running = total; // TODO: 실제 status 연동 시 교체

                double utilizationRate = total > 0
                        ? Math.round((double) running / total * 1000.0) / 10.0
                        : 0.0;

                for (Equipment eq : equipments) {
                    List<Alarm> activeAlarms = alarmRepository.findActiveAlarmsByEquipmentId(eq.getEquipmentId());
                    for (Alarm alarm : activeAlarms) {
                        alerts.add(ProcessDto.AlertItem.builder()
                                .alarmId(alarm.getAlarmId())
                                .sourceType(alarm.getSourceType().name())
                                .equipmentId(eq.getEquipmentId())
                                .equipmentName(eq.getEquipmentName())
                                .message(alarm.getMessage())
                                .severity(alarm.getSeverity().name())
                                .triggeredValue(alarm.getTriggeredValue())
                                .occurredAt(alarm.getOccurredAt())
                                .build());

                        if (lastUpdatedAt == null || alarm.getOccurredAt().isAfter(lastUpdatedAt)) {
                            lastUpdatedAt = alarm.getOccurredAt();
                        }
                    }
                }

                boolean hasErr  = alerts.stream().anyMatch(a -> "ERR".equals(a.getSeverity()));
                boolean hasWarn = alerts.stream().anyMatch(a -> "WARN".equals(a.getSeverity()));
                if (hasErr)       status = "CRITICAL";
                else if (hasWarn) status = "WARNING";

                items.add(ProcessDto.ProcessCardItem.builder()
                        .stepNo(process.getStepNo())
                        .processName(process.getProcessName())
                        .sortOrder((int) process.getSortOrder())
                        .hasEquipment(true)
                        .equipmentType(process.getEquipmentType())
                        .totalEquipment(total)
                        .runningEquipment(running)
                        .utilizationRate(utilizationRate)
                        .status(status)
                        .statusLabel(toStatusLabel(status))
                        .alerts(alerts)
                        .lastUpdatedAt(lastUpdatedAt)
                        .build());
            } else {
                items.add(ProcessDto.ProcessCardItem.builder()
                        .stepNo(process.getStepNo())
                        .processName(process.getProcessName())
                        .sortOrder((int) process.getSortOrder())
                        .hasEquipment(false)
                        .equipmentType(null)
                        .totalEquipment(null)
                        .runningEquipment(null)
                        .utilizationRate(null)
                        .status("NORMAL")
                        .statusLabel("정상")
                        .alerts(List.of())
                        .lastUpdatedAt(null)
                        .build());
            }
        }

        return ProcessDto.ProcessListResponse.builder()
                .updatedAt(LocalDateTime.now())
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public ProcessDto.PartnerResponse getPartner(String stepNo) {

        ProcessEntity process = processRepository.findById(stepNo)
                .orElseThrow(() -> new ReportException(ErrorCode.PROCESS_NOT_FOUND));

        if (process.getHasEquipment()) {
            throw new ReportException(ErrorCode.NOT_PARTNER_PROCESS);
        }

        Partner partner = partnerRepository.findById(stepNo)
                .orElseThrow(() -> new ReportException(ErrorCode.PROCESS_NOT_FOUND));

        // 재고 상태 계산
        String stockStatus;
        String stockStatusLabel;
        if (partner.getSafetyStock() == null) {
            stockStatus = "SUFFICIENT";
            stockStatusLabel = "충분";
        } else if (partner.getStockQty() >= partner.getSafetyStock() * 1.5) {
            stockStatus = "SUFFICIENT";
            stockStatusLabel = "충분";
        } else if (partner.getStockQty() >= partner.getSafetyStock()) {
            stockStatus = "WARNING";
            stockStatusLabel = "부족 주의";
        } else {
            stockStatus = "CRITICAL";
            stockStatusLabel = "부족";
        }

        // 납기 상태 계산
        boolean isDelayed = partner.getDeadline().isBefore(LocalDate.now());
        String deliveryStatus = isDelayed ? "DELAYED" : "ON_TIME";
        String deliveryStatusLabel = isDelayed ? "지연" : "정상";

        return ProcessDto.PartnerResponse.builder()
                .stepNo(process.getStepNo())
                .processName(process.getProcessName())
                .partner(ProcessDto.PartnerInfo.builder()
                        .companyName(partner.getCompanyName())
                        .productName(partner.getProductName())
                        .managerName(partner.getManagerName())
                        .contactTel(partner.getContactTel())
                        .contactEmail(partner.getContactEmail())
                        .build())
                .inventory(ProcessDto.InventoryInfo.builder()
                        .stockQty(partner.getStockQty())
                        .stockUnit(partner.getStockUnit())
                        .safetyStock(partner.getSafetyStock())
                        .stockStatus(stockStatus)
                        .stockStatusLabel(stockStatusLabel)
                        .build())
                .delivery(ProcessDto.DeliveryInfo.builder()
                        .deadline(partner.getDeadline().toString())
                        .deliveryStatus(deliveryStatus)
                        .deliveryStatusLabel(deliveryStatusLabel)
                        .delayReason(partner.getDelayReason())
                        .build())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }

    private String toStatusLabel(String status) {
        return switch (status) {
            case "CRITICAL" -> "이상";
            case "WARNING"  -> "경고";
            default         -> "정상";
        };
    }
}