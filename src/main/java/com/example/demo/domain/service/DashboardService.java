package com.example.demo.domain.service;

import com.example.demo.domain.dto.DashboardDto;
import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.AlarmStatus;
import com.example.demo.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final double OEE_TARGET = 75.0;
    private static final double YIELD_TARGET = 98.0;
    private static final double DEFECT_WARNING = 3.0;
    private static final double DEFECT_CRITICAL = 5.0;

    private final EquipmentRepository equipmentRepository;
    private final StatusChangeLogRepository statusChangeLogRepository;
    private final OeeMetricRepository oeeMetricRepository;
    private final ProcessRepository processRepository;
    private final AlarmRepository alarmRepository;

    // ── 1.1 설비 현황 KPI ──────────────────────────────────────────────────
    public DashboardDto.StatusResponse getStatus() {
        List<Equipment> equipments = equipmentRepository.findAll();

        int running = 0, alarm = 0, maintenance = 0, idle = 0;
        for (Equipment eq : equipments) {
            int status = getLatestStatus(eq.getEquipmentId());
            switch (status) {
                case 1 -> running++;
                case 2 -> alarm++;
                case 3 -> maintenance++;
                default -> idle++;
            }
        }
        int warning = countWarningEquipments(equipments);

        return DashboardDto.StatusResponse.builder()
                .total(equipments.size())
                .running(running)
                .warning(warning)
                .alarm(alarm)
                .maintenance(maintenance)
                .idle(idle)
                .updatedAt(OffsetDateTime.now(KST))
                .build();
    }

    // ── 1.2 OEE 요약 ───────────────────────────────────────────────────────
    public DashboardDto.OeeResponse getOee(String period) {
        LocalDateTime from = periodToFrom(period);
        List<OeeMetric> metrics = oeeMetricRepository.findAllSince(from);

        if (metrics.isEmpty()) {
            return DashboardDto.OeeResponse.builder()
                    .period(period).oee(null).target(OEE_TARGET)
                    .availability(null).performance(null).quality(null)
                    .trend(List.of()).updatedAt(OffsetDateTime.now(KST)).build();
        }

        OeeMetric latest = metrics.get(metrics.size() - 1);
        Optional<OeeMetric> prevOpt = oeeMetricRepository
                .findAllSince(from.minusDays(periodDays(period)))
                .stream().findFirst();

        List<DashboardDto.OeeTrendPoint> trend = metrics.stream()
                .map(m -> DashboardDto.OeeTrendPoint.builder()
                        .periodStart(toKst(m.getPeriodStart()))
                        .oee(m.getOee() != null ? round2(m.getOee() * 100) : null)
                        .build())
                .collect(Collectors.toList());

        return DashboardDto.OeeResponse.builder()
                .period(period)
                .oee(latest.getOee() != null ? round2(latest.getOee() * 100) : null)
                .target(OEE_TARGET)
                .prevOee(prevOpt.map(m -> m.getOee() != null ? round2(m.getOee() * 100) : null).orElse(null))
                .availability(latest.getAvailability() != null ? round2(latest.getAvailability() * 100) : null)
                .performance(latest.getPerformance() != null ? round2(latest.getPerformance() * 100) : null)
                .quality(latest.getQuality() != null ? round2(latest.getQuality() * 100) : null)
                .trend(trend)
                .updatedAt(OffsetDateTime.now(KST))
                .build();
    }

    // ── 1.3 공정별 이상률 ──────────────────────────────────────────────────
    public DashboardDto.DefectRateResponse getDefectRate(String period) {
        LocalDateTime from = periodToFrom(period);
        List<ProcessEntity> processes = processRepository.findAllByOrderBySortOrderAsc();

        List<DashboardDto.ProcessDefect> result = new ArrayList<>();
        for (ProcessEntity process : processes) {
            if (!process.getHasEquipment()) continue;

            List<Equipment> equipments = equipmentRepository.findByStepNo(process.getStepNo());
            if (equipments.isEmpty()) continue;

            List<OeeMetric> metrics = new ArrayList<>();
            for (Equipment eq : equipments) {
                metrics.addAll(oeeMetricRepository.findByEquipmentIdSince(eq.getEquipmentId(), from));
            }

            if (metrics.isEmpty()) continue;

            double avgQuality = metrics.stream()
                    .filter(m -> m.getQuality() != null)
                    .mapToDouble(OeeMetric::getQuality)
                    .average()
                    .orElse(1.0);
            double defectRate = round2((1.0 - avgQuality) * 100);

            String alertLevel = defectRate >= DEFECT_CRITICAL ? "CRITICAL"
                    : defectRate >= DEFECT_WARNING ? "WARNING" : "NORMAL";

            result.add(DashboardDto.ProcessDefect.builder()
                    .stepNo(process.getStepNo())
                    .processName(process.getProcessName())
                    .defectRate(defectRate)
                    .alertLevel(alertLevel)
                    .build());
        }

        return DashboardDto.DefectRateResponse.builder()
                .period(period)
                .processes(result)
                .updatedAt(OffsetDateTime.now(KST))
                .build();
    }

    // ── 1.4 테스트 양품률 ──────────────────────────────────────────────────
    public DashboardDto.YieldResponse getYield(String period) {
        LocalDateTime from = periodToFrom(period);
        List<OeeMetric> metrics = oeeMetricRepository.findAllSince(from);

        if (metrics.isEmpty()) {
            return DashboardDto.YieldResponse.builder()
                    .period(period).yield(null).target(YIELD_TARGET)
                    .trend(List.of()).updatedAt(OffsetDateTime.now(KST)).build();
        }

        OptionalDouble avgQuality = metrics.stream()
                .filter(m -> m.getQuality() != null)
                .mapToDouble(OeeMetric::getQuality)
                .average();

        double yieldVal = avgQuality.isPresent() ? round2(avgQuality.getAsDouble() * 100) : 0.0;

        Optional<OeeMetric> prevOpt = oeeMetricRepository
                .findAllSince(from.minusDays(periodDays(period)))
                .stream().findFirst();
        Double prevYield = prevOpt.map(m -> m.getQuality() != null ? round2(m.getQuality() * 100) : null).orElse(null);

        List<DashboardDto.YieldTrendPoint> trend = metrics.stream()
                .map(m -> DashboardDto.YieldTrendPoint.builder()
                        .periodStart(toKst(m.getPeriodStart()))
                        .yield(m.getQuality() != null ? round2(m.getQuality() * 100) : null)
                        .build())
                .collect(Collectors.toList());

        return DashboardDto.YieldResponse.builder()
                .period(period)
                .yield(yieldVal)
                .target(YIELD_TARGET)
                .prevYield(prevYield)
                .trend(trend)
                .updatedAt(OffsetDateTime.now(KST))
                .build();
    }

    // ── 1.5 설비 목록 (stepNo, status 필터) ───────────────────────────────
    public DashboardDto.EquipmentsResponse getEquipments(String stepNo, String status) {
        List<Equipment> all = stepNo != null
                ? equipmentRepository.findByStepNo(stepNo)
                : equipmentRepository.findAll();

        Map<String, String> processNameMap = processRepository.findAllByOrderBySortOrderAsc()
                .stream().collect(Collectors.toMap(ProcessEntity::getStepNo, ProcessEntity::getProcessName));

        List<DashboardDto.EquipmentItem> items = new ArrayList<>();
        for (Equipment eq : all) {
            int statusCode = getLatestStatus(eq.getEquipmentId());
            String statusStr = statusCodeToString(statusCode);

            // status 필터 적용
            if (status != null && !status.equalsIgnoreCase(statusStr)) continue;

            String statusLabel = statusCodeToLabel(statusCode);

            Optional<OeeMetric> latestOee = oeeMetricRepository.findLatestByEquipmentId(eq.getEquipmentId());
            Double oee = latestOee.map(m -> m.getOee() != null ? round2(m.getOee() * 100) : null).orElse(null);

            DashboardDto.AlarmSummary lastAlarm = null;
            List<Alarm> activeAlarms = alarmRepository.findActiveAlarmsByEquipmentId(eq.getEquipmentId(), LocalDateTime.now());
            if (!activeAlarms.isEmpty()) {
                Alarm a = activeAlarms.get(0);
                lastAlarm = DashboardDto.AlarmSummary.builder()
                        .alarmId(a.getAlarmId())
                        .sourceType(a.getSourceType().name())
                        .severity(a.getSeverity().name())
                        .message(a.getMessage())
                        .occurredAt(toKst(a.getOccurredAt()))
                        .build();
            }

            LocalDateTime updatedAt = statusChangeLogRepository
                    .findLatestByEquipmentId(eq.getEquipmentId())
                    .map(StatusChangeLog::getChangedAt)
                    .orElse(LocalDateTime.now());

            items.add(DashboardDto.EquipmentItem.builder()
                    .equipmentId(eq.getEquipmentId())
                    .equipmentName(eq.getEquipmentName())
                    .stepNo(eq.getStepNo())
                    .processName(processNameMap.getOrDefault(eq.getStepNo(), ""))
                    .status(statusStr)
                    .statusLabel(statusLabel)
                    .oee(oee)
                    .lastAlarm(lastAlarm)
                    .updatedAt(toKst(updatedAt))
                    .build());
        }

        return DashboardDto.EquipmentsResponse.builder()
                .total(items.size())
                .equipments(items)
                .build();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private int getLatestStatus(String equipmentId) {
        return statusChangeLogRepository.findLatestByEquipmentId(equipmentId)
                .map(s -> s.getNewStatus() != null ? (int) s.getNewStatus() : 0)
                .orElse(0);
    }

    private int countWarningEquipments(List<Equipment> equipments) {
        int count = 0;
        for (Equipment eq : equipments) {
            List<Alarm> active = alarmRepository.findActiveAlarmsByEquipmentId(eq.getEquipmentId(), LocalDateTime.now());
            boolean hasWarn = active.stream().anyMatch(a ->
                    a.getSeverity().name().equals("WARN") && a.getStatus() != AlarmStatus.DONE);
            if (hasWarn) count++;
        }
        return count;
    }

    private LocalDateTime periodToFrom(String period) {
        return switch (period) {
            case "week"  -> LocalDateTime.now().minusDays(7);
            case "month" -> LocalDateTime.now().minusDays(30);
            default      -> LocalDateTime.now().toLocalDate().atStartOfDay();
        };
    }

    private long periodDays(String period) {
        return switch (period) {
            case "week"  -> 7;
            case "month" -> 30;
            default      -> 1;
        };
    }

    private String statusCodeToString(int code) {
        return switch (code) {
            case 1  -> "RUN";
            case 2  -> "ALARM";
            case 3  -> "PM";
            default -> "IDLE";
        };
    }

    private String statusCodeToLabel(int code) {
        return switch (code) {
            case 1  -> "가동";
            case 2  -> "알람";
            case 3  -> "PM";
            default -> "대기";
        };
    }

    private OffsetDateTime toKst(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(KST).toOffsetDateTime();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}