package com.example.demo.domain.service;

import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.EquipmentDto;
import com.example.demo.domain.entity.*;
import com.example.demo.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ProcessRepository processRepository;
    private final StatusChangeLogRepository statusChangeLogRepository;
    private final EquipmentParameterRepository equipmentParameterRepository;
    private final EquipmentMeasurementRepository equipmentMeasurementRepository;
    private final AlarmRepository alarmRepository;
    private final PmScheduleRepository pmScheduleRepository;
    private final ManagerNoteRepository managerNoteRepository;

    // ─── 4.1 공정별 설비 목록 ──────────────────
    @Transactional(readOnly = true)
    public EquipmentDto.EquipmentListResponse getEquipmentList(String stepNo) {

        ProcessEntity process = processRepository.findById(stepNo)
                .orElseThrow(() -> new ReportException(ErrorCode.PROCESS_NOT_FOUND));

        List<Equipment> equipments = equipmentRepository.findByStepNo(stepNo);

        List<EquipmentDto.EquipmentItem> items = equipments.stream()
                .map(eq -> {
                    int currentStatus = statusChangeLogRepository
                            .findLatestByEquipmentId(eq.getEquipmentId())
                            .map(s -> (int) s.getNewStatus())
                            .orElse(0);

                    return EquipmentDto.EquipmentItem.builder()
                            .equipmentId(eq.getEquipmentId())
                            .equipmentName(eq.getEquipmentName())
                            .unitNo((int) eq.getUnitNo())
                            .currentStatus(currentStatus)
                            .currentStatusLabel(toStatusLabel(currentStatus))
                            .totalRunningHours(eq.getTotalRunningHours())
                            .build();
                })
                .toList();

        return EquipmentDto.EquipmentListResponse.builder()
                .stepNo(process.getStepNo())
                .processName(process.getProcessName())
                .items(items)
                .build();
    }

    // ─── 4.2 설비 파라미터·측정값 ──────────────
    @Transactional(readOnly = true)
    public EquipmentDto.ParameterListResponse getParameters(String equipmentId, String period) {

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        int currentStatus = statusChangeLogRepository
                .findLatestByEquipmentId(equipmentId)
                .map(s -> (int) s.getNewStatus())
                .orElse(0);

        LocalDateTime from = switch (period != null ? period : "1h") {
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d"  -> LocalDateTime.now().minusDays(7);
            default    -> LocalDateTime.now().minusHours(1);
        };

        List<EquipmentParameter> params = equipmentParameterRepository.findByEquipmentId(equipmentId);

        String overallStatus = "NORMAL";

        List<EquipmentDto.ParameterItem> paramItems = new ArrayList<>();
        for (EquipmentParameter param : params) {

            EquipmentMeasurement latest = equipmentMeasurementRepository
                    .findLatestByParamId(param.getParamId()).orElse(null);

            Double latestValue = latest != null ? latest.getMeasuredValue() : null;
            LocalDateTime latestAt = latest != null ? latest.getMeasuredAt() : null;

            String paramStatus = "NORMAL";
            if (latestValue != null && param.getNormalMin() != null && param.getNormalMax() != null) {
                double range = param.getNormalMax() - param.getNormalMin();
                if (latestValue < param.getNormalMin() || latestValue > param.getNormalMax()) {
                    paramStatus = "CRITICAL";
                } else if (latestValue < param.getNormalMin() + range * 0.1
                        || latestValue > param.getNormalMax() - range * 0.1) {
                    paramStatus = "WARNING";
                }
            }

            if ("CRITICAL".equals(paramStatus)) overallStatus = "CRITICAL";
            else if ("WARNING".equals(paramStatus) && !"CRITICAL".equals(overallStatus)) overallStatus = "WARNING";

            List<EquipmentDto.MeasurementItem> history = equipmentMeasurementRepository
                    .findByParamIdAndMeasuredAtAfterOrderByMeasuredAtAsc(param.getParamId(), from)
                    .stream()
                    .map(m -> EquipmentDto.MeasurementItem.builder()
                            .value(m.getMeasuredValue())
                            .measuredAt(m.getMeasuredAt())
                            .build())
                    .toList();

            paramItems.add(EquipmentDto.ParameterItem.builder()
                    .paramId(param.getParamId())
                    .tagCode(param.getTagCode())
                    .tagName(param.getTagName())
                    .unit(param.getUnit())
                    .normalMin(param.getNormalMin())
                    .normalMax(param.getNormalMax())
                    .latestValue(latestValue)
                    .latestAt(latestAt)
                    .paramStatus(paramStatus)
                    .history(history)
                    .build());
        }

        return EquipmentDto.ParameterListResponse.builder()
                .equipmentId(equipment.getEquipmentId())
                .equipmentName(equipment.getEquipmentName())
                .currentStatus(currentStatus)
                .currentStatusLabel(toStatusLabel(currentStatus))
                .overallStatus(overallStatus)
                .overallStatusLabel(toOverallStatusLabel(overallStatus))
                .parameters(paramItems)
                .build();
    }

    // ─── 4.3 설비 알람 ────────────────────────
    @Transactional(readOnly = true)
    public EquipmentDto.AlarmListResponse getAlarms(String equipmentId, String status) {

        equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        List<Alarm> alarms = "all".equals(status)
                ? alarmRepository.findAllAlarmsByEquipmentId(equipmentId)
                : alarmRepository.findActiveAlarmsByEquipmentId(equipmentId);

        List<EquipmentParameter> params = equipmentParameterRepository.findByEquipmentId(equipmentId);
        Map<Long, String> paramTagMap = params.stream()
                .collect(Collectors.toMap(EquipmentParameter::getParamId, EquipmentParameter::getTagName));

        List<EquipmentDto.AlarmItem> items = alarms.stream()
                .map(a -> EquipmentDto.AlarmItem.builder()
                        .alarmId(a.getAlarmId())
                        .severity(a.getSeverity().name())
                        .tagName(paramTagMap.get(a.getEquipmentParamId()))
                        .message(a.getMessage())
                        .triggeredValue(a.getTriggeredValue())
                        .occurrenceCount(a.getOccurrenceCount())
                        .status(a.getStatus().name())
                        .statusLabel(toAlarmStatusLabel(a.getStatus().name()))
                        .occurredAt(a.getOccurredAt())
                        .build())
                .toList();

        return EquipmentDto.AlarmListResponse.builder()
                .equipmentId(equipmentId)
                .items(items)
                .build();
    }

    // ─── 4.4 설비 이벤트 로그 ─────────────────
    @Transactional(readOnly = true)
    public EquipmentDto.EventListResponse getEvents(String equipmentId, int page, int size) {

        equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        List<EquipmentDto.EventItem> allEvents = new ArrayList<>();

        // 알람 이벤트
        for (Alarm a : alarmRepository.findAllAlarmsByEquipmentId(equipmentId)) {
            allEvents.add(EquipmentDto.EventItem.builder()
                    .eventType("ALARM")
                    .eventLabel("알람 발생")
                    .message(a.getMessage())
                    .severity(a.getSeverity().name())
                    .severityLabel(toSeverityLabel(a.getSeverity().name()))
                    .occurredAt(a.getOccurredAt())
                    .build());
        }

        // 상태 변경 이벤트
        for (StatusChangeLog s : statusChangeLogRepository.findByEquipmentId(equipmentId)) {
            allEvents.add(EquipmentDto.EventItem.builder()
                    .eventType("STATUS_CHANGE")
                    .eventLabel("상태 변경")
                    .message("설비가 " + toStatusLabel(s.getNewStatus()) + " 상태로 변경되었습니다")
                    .severity("INFO")
                    .severityLabel("정상")
                    .occurredAt(s.getChangedAt())
                    .build());
        }

        // PM 이벤트
        for (PmSchedule p : pmScheduleRepository.findByEquipmentId(equipmentId)) {
            allEvents.add(EquipmentDto.EventItem.builder()
                    .eventType("PM")
                    .eventLabel("정기 점검")
                    .message("정기 점검 일정이 도래했습니다")
                    .severity("INFO")
                    .severityLabel("정상")
                    .occurredAt(p.getScheduledAt())
                    .build());
        }

        // 시간 내림차순 정렬
        allEvents.sort(Comparator.comparing(EquipmentDto.EventItem::getOccurredAt).reversed());

        // 페이징
        long total = allEvents.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int fromIdx = Math.min((page - 1) * size, allEvents.size());
        int toIdx = Math.min(fromIdx + size, allEvents.size());
        List<EquipmentDto.EventItem> pagedItems = allEvents.subList(fromIdx, toIdx);

        return EquipmentDto.EventListResponse.builder()
                .equipmentId(equipmentId)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .items(pagedItems)
                .build();
    }

    // ─── 4.5 관리자 의견 추가 ─────────────────
    @Transactional
    public EquipmentDto.NoteResponse addNote(String equipmentId, String noteText, String userId) {

        equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (noteText == null || noteText.isBlank()) {
            throw new ReportException(ErrorCode.BAD_REQUEST);
        }
        if (noteText.length() > 500) {
            throw new ReportException(ErrorCode.BAD_REQUEST);
        }

        ManagerNote note = ManagerNote.builder()
                .userId(userId)
                .equipmentId(equipmentId)
                .noteText(noteText)
                .build();

        ManagerNote saved = managerNoteRepository.save(note);

        return EquipmentDto.NoteResponse.builder()
                .noteId(saved.getNoteId())
                .userId(saved.getUserId())
                .equipmentId(saved.getEquipmentId())
                .noteText(saved.getNoteText())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // ─── 4.6 관리자 의견 목록 조회 ───────────────
    @Transactional(readOnly = true)
    public EquipmentDto.NoteListResponse getNotes(String equipmentId) {

        equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        List<EquipmentDto.NoteResponse> items = managerNoteRepository
                .findByEquipmentIdOrderByCreatedAtDesc(equipmentId)
                .stream()
                .map(n -> EquipmentDto.NoteResponse.builder()
                        .noteId(n.getNoteId())
                        .userId(n.getUserId())
                        .equipmentId(n.getEquipmentId())
                        .noteText(n.getNoteText())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();

        return EquipmentDto.NoteListResponse.builder()
                .equipmentId(equipmentId)
                .items(items)
                .build();
    }

    // ─── 라벨 변환 헬퍼 ───────────────────────
    private String toStatusLabel(int status) {
        return switch (status) {
            case 0 -> "IDLE";
            case 1 -> "RUN";
            case 2 -> "ALARM";
            case 3 -> "PM";
            default -> "IDLE";
        };
    }

    private String toStatusLabel(Byte status) {
        return status == null ? "IDLE" : toStatusLabel((int) (byte) status);
    }

    private String toOverallStatusLabel(String status) {
        return switch (status) {
            case "CRITICAL" -> "이상";
            case "WARNING"  -> "경고";
            default         -> "정상";
        };
    }

    private String toAlarmStatusLabel(String status) {
        return switch (status) {
            case "NEW"         -> "미확인";
            case "ACK"         -> "확인";
            case "IN_PROGRESS" -> "조치 중";
            case "DONE"        -> "완료";
            default            -> "미확인";
        };
    }

    private String toSeverityLabel(String severity) {
        return switch (severity) {
            case "ERR"  -> "이상";
            case "WARN" -> "경고";
            default     -> "정상";
        };
    }
}