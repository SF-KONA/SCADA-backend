package com.example.demo.domain.service;

import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.AlarmDto;
import com.example.demo.domain.dto.AlarmQueryParams;
import com.example.demo.domain.entity.Alarm;
import com.example.demo.domain.entity.EquipmentParameter;
import com.example.demo.domain.entity.Equipment;
import com.example.demo.domain.enums.AlarmStatus;
import com.example.demo.domain.enums.AlarmSourceType;
import com.example.demo.domain.repository.AlarmRepository;
import com.example.demo.domain.repository.EquipmentParameterRepository;
import com.example.demo.domain.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final AlarmRepository alarmRepository;
    private final EquipmentParameterRepository equipmentParamRepo;
    private final EquipmentRepository equipmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─────────────────────────────────────────────
    // 2.1 알람 목록 조회
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AlarmDto.PageResponse getAlarms(AlarmQueryParams params) {
        params.validate();

        Pageable pageable = PageRequest.of(params.getPage() - 1, params.getSize());

        Page<Alarm> page = alarmRepository.findByFilters(
                params.getSourceType(),
                params.parsedSeverities(),
                params.parsedStatuses(),
                params.getEquipmentId(),
                params.getStepNo(),
                params.getFrom(),
                params.getTo(),
                pageable
        );

        List<AlarmDto.ListItem> items = page.getContent()
                .stream()
                .map(this::toListItem)
                .toList();

        return AlarmDto.PageResponse.builder()
                .total(page.getTotalElements())
                .page(params.getPage())
                .size(params.getSize())
                .totalPages(page.getTotalPages())
                .items(items)
                .build();
    }

    // ─────────────────────────────────────────────
    // 2.2 알람 상세 조회
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AlarmDto.Detail getAlarmDetail(Long alarmId) {
        Alarm alarm = findAlarmOrThrow(alarmId);
        return toDetail(alarm);
    }

    // ─────────────────────────────────────────────
    // 2.3 알람 ACK (NEW → ACK)
    // ─────────────────────────────────────────────
    @Transactional
    public AlarmDto.AckResponse ackAlarm(Long alarmId, String userId) {
        Alarm alarm = findAlarmOrThrow(alarmId);

        if (alarm.getStatus() != AlarmStatus.NEW) {
            throw new ReportException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "현재 상태: " + alarm.getStatus() + " (ACK는 NEW 상태에서만 가능)");
        }

        LocalDateTime now = LocalDateTime.now();

        // 엔티티 불변 → 새 인스턴스로 저장
        Alarm updated = Alarm.builder()
                .alarmId(alarm.getAlarmId())
                .sourceType(alarm.getSourceType())
                .equipmentParamId(alarm.getEquipmentParamId())
                .environmentParamId(alarm.getEnvironmentParamId())
                .occurredAt(alarm.getOccurredAt())
                .severity(alarm.getSeverity())
                .message(alarm.getMessage())
                .status(AlarmStatus.ACK)
                .ackUserId(userId)
                .ackAt(now)
                .startUserId(alarm.getStartUserId())
                .startAt(alarm.getStartAt())
                .doneUserId(alarm.getDoneUserId())
                .doneAt(alarm.getDoneAt())
                .lastOccurredAt(alarm.getLastOccurredAt())
                .occurrenceCount(alarm.getOccurrenceCount())
                .triggeredValue(alarm.getTriggeredValue())
                .doneComment(alarm.getDoneComment())
                .build();

        alarmRepository.save(updated);

        // ALARM_ACK 이벤트 broadcast
        broadcastAlarmAck(alarm, userId);

        return AlarmDto.AckResponse.builder()
                .alarmId(alarm.getAlarmId())
                .status(AlarmStatus.ACK)
                .ackUserId(userId)
                .ackAt(toKst(now))
                .build();
    }

    private void broadcastAlarmAck(Alarm alarm, String userId) {
        if (alarm.getSourceType() != AlarmSourceType.EQP) return;

        String equipmentId = null;
        String equipmentName = null;
        if (alarm.getEquipmentParamId() != null) {
            Optional<EquipmentParameter> ep = equipmentParamRepo.findById(alarm.getEquipmentParamId());
            if (ep.isPresent()) {
                equipmentId = ep.get().getEquipmentId();
                equipmentName = equipmentRepository.findById(equipmentId)
                        .map(Equipment::getEquipmentName).orElse(null);
            }
        }

        Map<String, Object> alarmMap = new LinkedHashMap<>();
        alarmMap.put("alarmId", alarm.getAlarmId());
        alarmMap.put("sourceType", alarm.getSourceType().name());
        alarmMap.put("message", alarm.getMessage());
        alarmMap.put("severity", alarm.getSeverity().name());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ALARM_ACK");
        payload.put("equipmentId", equipmentId);
        payload.put("equipmentName", equipmentName);
        payload.put("status", null);
        payload.put("alarm", alarmMap);
        payload.put("ackedBy", userId);
        payload.put("timestamp", ZonedDateTime.now(KST).format(FORMATTER));

        messagingTemplate.convertAndSend("/topic/dashboard", payload);
    }

    // ─────────────────────────────────────────────
    // 2.4 처리 시작 (ACK → IN_PROGRESS)
    // ─────────────────────────────────────────────
    @Transactional
    public AlarmDto.StartResponse startAlarm(Long alarmId, String userId) {
        Alarm alarm = findAlarmOrThrow(alarmId);

        if (alarm.getStatus() != AlarmStatus.ACK) {
            throw new ReportException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "현재 상태: " + alarm.getStatus() + " (처리 시작은 ACK 상태에서만 가능)");
        }

        LocalDateTime now = LocalDateTime.now();

        Alarm updated = Alarm.builder()
                .alarmId(alarm.getAlarmId())
                .sourceType(alarm.getSourceType())
                .equipmentParamId(alarm.getEquipmentParamId())
                .environmentParamId(alarm.getEnvironmentParamId())
                .occurredAt(alarm.getOccurredAt())
                .severity(alarm.getSeverity())
                .message(alarm.getMessage())
                .status(AlarmStatus.IN_PROGRESS)
                .ackUserId(alarm.getAckUserId())
                .ackAt(alarm.getAckAt())
                .startUserId(userId)
                .startAt(now)
                .doneUserId(alarm.getDoneUserId())
                .doneAt(alarm.getDoneAt())
                .lastOccurredAt(alarm.getLastOccurredAt())
                .occurrenceCount(alarm.getOccurrenceCount())
                .triggeredValue(alarm.getTriggeredValue())
                .doneComment(alarm.getDoneComment())
                .build();

        alarmRepository.save(updated);

        return AlarmDto.StartResponse.builder()
                .alarmId(alarm.getAlarmId())
                .status(AlarmStatus.IN_PROGRESS)
                .startUserId(userId)
                .startAt(toKst(now))
                .build();
    }

    // ─────────────────────────────────────────────
    // 2.5 처리 완료 (IN_PROGRESS → DONE)
    // ─────────────────────────────────────────────
    @Transactional
    public AlarmDto.DoneResponse doneAlarm(Long alarmId, String userId, String comment) {
        Alarm alarm = findAlarmOrThrow(alarmId);

        if (alarm.getStatus() != AlarmStatus.IN_PROGRESS) {
            throw new ReportException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "현재 상태: " + alarm.getStatus() + " (처리 완료는 IN_PROGRESS 상태에서만 가능)");
        }

        LocalDateTime now = LocalDateTime.now();

        Alarm updated = Alarm.builder()
                .alarmId(alarm.getAlarmId())
                .sourceType(alarm.getSourceType())
                .equipmentParamId(alarm.getEquipmentParamId())
                .environmentParamId(alarm.getEnvironmentParamId())
                .occurredAt(alarm.getOccurredAt())
                .severity(alarm.getSeverity())
                .message(alarm.getMessage())
                .status(AlarmStatus.DONE)
                .ackUserId(alarm.getAckUserId())
                .ackAt(alarm.getAckAt())
                .startUserId(alarm.getStartUserId())
                .startAt(alarm.getStartAt())
                .doneUserId(userId)
                .doneAt(now)
                .lastOccurredAt(alarm.getLastOccurredAt())
                .occurrenceCount(alarm.getOccurrenceCount())
                .triggeredValue(alarm.getTriggeredValue())
                .doneComment(comment)
                .build();

        alarmRepository.save(updated);

        return AlarmDto.DoneResponse.builder()
                .alarmId(alarm.getAlarmId())
                .status(AlarmStatus.DONE)
                .doneUserId(userId)
                .doneAt(toKst(now))
                .doneComment(comment)
                .build();
    }

    // ─────────────────────────────────────────────
    // 2.6 미확인 비상 알람 조회
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AlarmDto.EmergencyResponse getEmergencyAlarms() {
        List<Alarm> alarms = alarmRepository.findEmergencyAlarms();

        List<AlarmDto.EmergencyItem> items = alarms.stream()
                .map(this::toEmergencyItem)
                .toList();

        return AlarmDto.EmergencyResponse.builder()
                .count(items.size())
                .items(items)
                .build();
    }

    // ─────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────

    private Alarm findAlarmOrThrow(Long alarmId) {
        return alarmRepository.findById(alarmId)
                .orElseThrow(() -> new ReportException(ErrorCode.ALARM_NOT_FOUND,
                        "alarmId=" + alarmId));
    }

    private AlarmDto.ListItem toListItem(Alarm alarm) {
        ParamMeta meta = resolveParamMeta(alarm);

        return AlarmDto.ListItem.builder()
                .alarmId(alarm.getAlarmId())
                .sourceType(alarm.getSourceType())
                .equipmentId(meta.equipmentId())
                .equipmentName(meta.equipmentName())
                .stepNo(meta.stepNo())
                .processName(meta.processName())
                .zoneCode(meta.zoneCode())
                .severity(alarm.getSeverity())
                .message(alarm.getMessage())
                .status(alarm.getStatus())
                .occurredAt(toKst(alarm.getOccurredAt()))
                .lastOccurredAt(toKst(alarm.getLastOccurredAt()))
                .occurrenceCount(alarm.getOccurrenceCount())
                .ackUserId(alarm.getAckUserId())
                .ackAt(toKst(alarm.getAckAt()))
                .startUserId(alarm.getStartUserId())
                .startAt(toKst(alarm.getStartAt()))
                .doneUserId(alarm.getDoneUserId())
                .doneAt(toKst(alarm.getDoneAt()))
                .build();
    }

    private AlarmDto.Detail toDetail(Alarm alarm) {
        ParamMeta meta = resolveParamMeta(alarm);

        return AlarmDto.Detail.builder()
                .alarmId(alarm.getAlarmId())
                .sourceType(alarm.getSourceType())
                .equipmentId(meta.equipmentId())
                .equipmentName(meta.equipmentName())
                .stepNo(meta.stepNo())
                .processName(meta.processName())
                .paramId(meta.paramId())
                .tagCode(meta.tagCode())
                .tagName(meta.tagName())
                .unit(meta.unit())
                .normalMin(meta.normalMin())
                .normalMax(meta.normalMax())
                .zoneCode(meta.zoneCode())
                .severity(alarm.getSeverity())
                .message(alarm.getMessage())
                .status(alarm.getStatus())
                .occurredAt(toKst(alarm.getOccurredAt()))
                .lastOccurredAt(toKst(alarm.getLastOccurredAt()))
                .occurrenceCount(alarm.getOccurrenceCount())
                .triggeredValue(alarm.getTriggeredValue())
                .ackUserId(alarm.getAckUserId())
                .ackAt(toKst(alarm.getAckAt()))
                .startUserId(alarm.getStartUserId())
                .startAt(toKst(alarm.getStartAt()))
                .doneUserId(alarm.getDoneUserId())
                .doneAt(toKst(alarm.getDoneAt()))
                .doneComment(alarm.getDoneComment())
                .build();
    }

    private AlarmDto.EmergencyItem toEmergencyItem(Alarm alarm) {
        ParamMeta meta = resolveParamMeta(alarm);

        return AlarmDto.EmergencyItem.builder()
                .alarmId(alarm.getAlarmId())
                .sourceType(alarm.getSourceType())
                .equipmentId(meta.equipmentId())
                .equipmentName(meta.equipmentName())
                .stepNo(meta.stepNo())
                .processName(meta.processName())
                .zoneCode(meta.zoneCode())
                .tagCode(meta.tagCode())
                .tagName(meta.tagName())
                .unit(meta.unit())
                .triggeredValue(alarm.getTriggeredValue())
                .normalMin(meta.normalMin())
                .normalMax(meta.normalMax())
                .severity(alarm.getSeverity())
                .message(alarm.getMessage())
                .status(alarm.getStatus())
                .occurredAt(toKst(alarm.getOccurredAt()))
                .occurrenceCount(alarm.getOccurrenceCount())
                .build();
    }

    /**
     * sourceType에 따라 파라미터 메타(장비명, stepNo 등)를 조회하는 헬퍼.
     * EQP: EquipmentParameter → Equipment 조인
     * ENV: EnvironmentParameter에 stepNo/zoneCode 없으므로 null 처리
     */
    private ParamMeta resolveParamMeta(Alarm alarm) {
        if (alarm.getSourceType() == com.example.demo.domain.enums.AlarmSourceType.EQP
                && alarm.getEquipmentParamId() != null) {

            Optional<EquipmentParameter> epOpt = equipmentParamRepo.findById(alarm.getEquipmentParamId());
            if (epOpt.isPresent()) {
                EquipmentParameter ep = epOpt.get();
                Optional<Equipment> eqOpt = equipmentRepository.findById(ep.getEquipmentId());
                String eqName      = eqOpt.map(Equipment::getEquipmentName).orElse(null);
                String stepNo      = eqOpt.map(Equipment::getStepNo).orElse(null);
                String processName = resolveProcessName(stepNo);

                return new ParamMeta(
                        ep.getEquipmentId(),
                        eqName,
                        stepNo,
                        processName,
                        null,             // zoneCode (EQP에는 없음)
                        ep.getParamId(),
                        ep.getTagCode(),
                        ep.getTagName(),
                        ep.getUnit(),
                        ep.getNormalMin(),
                        ep.getNormalMax()
                );
            }
        }

        // ENV 알람은 stepNo/zoneCode 필드 없으므로 null 반환
        return ParamMeta.empty();
    }

    /** stepNo → 공정명 매핑 */
    private String resolveProcessName(String stepNo) {
        if (stepNo == null) return null;
        return switch (stepNo) {
            case "01" -> "Wafer 제조";
            case "02" -> "산화공정";
            case "03" -> "포토공정";
            case "04" -> "식각공정";
            case "05" -> "박막증착";
            case "06" -> "금속배선";
            case "07" -> "테스트";
            case "08" -> "패키징";
            default -> null;
        };
    }

    private OffsetDateTime toKst(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(KST).toOffsetDateTime();
    }

    /** 파라미터 메타 조회 결과 레코드 */
    private record ParamMeta(
            String equipmentId,
            String equipmentName,
            String stepNo,
            String processName,
            String zoneCode,
            Long paramId,
            String tagCode,
            String tagName,
            String unit,
            Double normalMin,
            Double normalMax
    ) {
        static ParamMeta empty() {
            return new ParamMeta(null, null, null, null, null, null, null, null, null, null, null);
        }
    }
}