package com.example.demo.domain.scheduler;

import com.example.demo.domain.entity.Equipment;
import com.example.demo.domain.entity.StatusChangeLog;
import com.example.demo.domain.repository.EquipmentRepository;
import com.example.demo.domain.repository.StatusChangeLogRepository;
import lombok.RequiredArgsConstructor;
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
public class DashboardBroadcastScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final SimpMessagingTemplate messagingTemplate;
    private final StatusChangeLogRepository statusLogRepository;
    private final EquipmentRepository equipmentRepository;

    private LocalDateTime lastBroadcastAt = LocalDateTime.now();

    @Scheduled(fixedDelay = 5000)
    public void broadcastStatusChanges() {
        LocalDateTime since = lastBroadcastAt;
        lastBroadcastAt = LocalDateTime.now();

        List<StatusChangeLog> recentChanges = statusLogRepository.findByChangedAtAfter(since);
        if (recentChanges.isEmpty()) return;

        for (StatusChangeLog log : recentChanges) {
            Optional<Equipment> eqOpt = equipmentRepository.findById(log.getEquipmentId());
            if (eqOpt.isEmpty()) continue;

            Equipment eq = eqOpt.get();
            String status = statusCodeToString(log.getNewStatus());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "STATUS_CHANGE");
            payload.put("equipmentId", log.getEquipmentId());
            payload.put("equipmentName", eq.getEquipmentName());
            payload.put("status", status);
            payload.put("alarm", null);
            payload.put("timestamp", ZonedDateTime.now(KST).format(FORMATTER));

            messagingTemplate.convertAndSend("/topic/dashboard", payload);
        }
    }

    private String statusCodeToString(Byte code) {
        if (code == null) return "IDLE";
        return switch (code) {
            case 1 -> "RUN";
            case 2 -> "ALARM";
            case 3 -> "PM";
            default -> "IDLE";
        };
    }
}
