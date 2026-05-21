package com.example.demo.domain.scheduler;

import com.example.demo.domain.entity.Alarm;
import com.example.demo.domain.entity.EquipmentMeasurement;
import com.example.demo.domain.entity.EquipmentParameter;
import com.example.demo.domain.entity.StatusChangeLog;
import com.example.demo.domain.enums.AlarmSourceType;
import com.example.demo.domain.enums.AlarmStatus;
import com.example.demo.domain.enums.AlarmSeverity;
import com.example.demo.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmDetectionScheduler {

    private static final String TARGET_EQUIPMENT_ID = "FURN_01";
    private static final String TARGET_TAG_CODE     = "FURN_01_TEMP";
    private static final double WARN_THRESHOLD      = 1100.0;
    private static final double ERR_THRESHOLD       = 1150.0;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final EquipmentParameterRepository parameterRepository;
    private final EquipmentMeasurementRepository measurementRepository;
    private final AlarmRepository alarmRepository;
    private final StatusChangeLogRepository statusChangeLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 30000)
    public void detectAlarms() {
        try {
            Optional<EquipmentParameter> paramOpt = parameterRepository
                    .findByEquipmentId(TARGET_EQUIPMENT_ID)
                    .stream()
                    .filter(p -> TARGET_TAG_CODE.equals(p.getTagCode()))
                    .findFirst();

            if (paramOpt.isEmpty()) return;
            EquipmentParameter param = paramOpt.get();

            LocalDateTime now = LocalDateTime.now();

            Optional<EquipmentMeasurement> latestOpt =
                    measurementRepository.findLatestByParamIdBeforeNow(param.getParamId(), now);
            if (latestOpt.isEmpty()) return;

            double value = latestOpt.get().getMeasuredValue();

            List<Alarm> activeAlarms = alarmRepository
                    .findActiveAlarmsByEquipmentId(TARGET_EQUIPMENT_ID, now);

            boolean hasErrAlarm  = activeAlarms.stream()
                    .anyMatch(a -> a.getSeverity() == AlarmSeverity.ERR);
            boolean hasWarnAlarm = activeAlarms.stream()
                    .anyMatch(a -> a.getSeverity() == AlarmSeverity.WARN);

            if (value > ERR_THRESHOLD) {
                if (!hasErrAlarm) {
                    Alarm alarm = Alarm.builder()
                            .sourceType(AlarmSourceType.EQP)
                            .equipmentParamId(param.getParamId())
                            .occurredAt(now)
                            .severity(AlarmSeverity.ERR)
                            .message(String.format("[비상] FURN_01 공정 온도 이상: %.1f°C (임계치 %.0f°C 초과)",
                                    value, ERR_THRESHOLD))
                            .status(AlarmStatus.NEW)
                            .lastOccurredAt(now)
                            .occurrenceCount(1)
                            .triggeredValue(value)
                            .build();
                    alarmRepository.save(alarm);
                    updateEquipmentStatus(TARGET_EQUIPMENT_ID, (byte) 1, (byte) 2, now);
                    broadcastStatusChange(TARGET_EQUIPMENT_ID, "ALARM", alarm);
                    broadcastAlarm(alarm, value);
                    log.warn("[AlarmDetection] FURN_01 ERR 알람 발생: {}°C", value);
                }
            } else if (value > WARN_THRESHOLD) {
                if (!hasWarnAlarm && !hasErrAlarm) {
                    Alarm alarm = Alarm.builder()
                            .sourceType(AlarmSourceType.EQP)
                            .equipmentParamId(param.getParamId())
                            .occurredAt(now)
                            .severity(AlarmSeverity.WARN)
                            .message(String.format("FURN_01 공정 온도 경고: %.1f°C (상한 %.0f°C 초과)",
                                    value, WARN_THRESHOLD))
                            .status(AlarmStatus.NEW)
                            .lastOccurredAt(now)
                            .occurrenceCount(1)
                            .triggeredValue(value)
                            .build();
                    alarmRepository.save(alarm);
                    updateEquipmentStatus(TARGET_EQUIPMENT_ID, (byte) 1, (byte) 3, now);
                    broadcastStatusChange(TARGET_EQUIPMENT_ID, "ALARM", alarm);
                    broadcastAlarm(alarm, value);
                    log.warn("[AlarmDetection] FURN_01 WARN 알람 발생: {}°C", value);
                }
            }

        } catch (Exception e) {
            log.error("[AlarmDetection] 알람 감지 중 오류", e);
        }
    }

    private void updateEquipmentStatus(String equipmentId, byte prevStatus, byte newStatus,
                                       LocalDateTime changedAt) {
        Optional<StatusChangeLog> latestLog = statusChangeLogRepository.findLatestByEquipmentId(equipmentId);
        if (latestLog.isPresent() && latestLog.get().getNewStatus() != null
                && latestLog.get().getNewStatus() == newStatus) {
            return;
        }
        StatusChangeLog log = StatusChangeLog.builder()
                .equipmentId(equipmentId)
                .prevStatus(prevStatus)
                .newStatus(newStatus)
                .changedAt(changedAt)
                .build();
        statusChangeLogRepository.save(log);
    }

    private void broadcastStatusChange(String equipmentId, String status, Alarm alarm) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "STATUS_CHANGE");
        payload.put("equipmentId", equipmentId);
        payload.put("status", status);
        payload.put("alarm", Map.of(
                "alarmId",    alarm.getAlarmId() != null ? alarm.getAlarmId() : 0,
                "severity",   alarm.getSeverity().name(),
                "message",    alarm.getMessage(),
                "occurredAt", ZonedDateTime.now(KST).format(FORMATTER),
                "sourceType", alarm.getSourceType().name()
        ));
        payload.put("timestamp", ZonedDateTime.now(KST).format(FORMATTER));
        messagingTemplate.convertAndSend("/topic/dashboard", payload);
    }

    private void broadcastAlarm(Alarm alarm, double value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ALARM");
        payload.put("equipmentId", TARGET_EQUIPMENT_ID);
        payload.put("alarm", Map.of(
                "alarmId",    alarm.getAlarmId() != null ? alarm.getAlarmId() : 0,
                "severity",   alarm.getSeverity().name(),
                "message",    alarm.getMessage(),
                "occurredAt", ZonedDateTime.now(KST).format(FORMATTER),
                "sourceType", alarm.getSourceType().name()
        ));
        payload.put("timestamp", ZonedDateTime.now(KST).format(FORMATTER));
        messagingTemplate.convertAndSend("/topic/dashboard", payload);
    }
}