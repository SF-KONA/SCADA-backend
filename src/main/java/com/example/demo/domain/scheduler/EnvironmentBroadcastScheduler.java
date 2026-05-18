package com.example.demo.domain.scheduler;

import com.example.demo.domain.entity.EnvironmentMeasurement;
import com.example.demo.domain.entity.EnvironmentParameter;
import com.example.demo.domain.entity.EnvironmentSensor;
import com.example.demo.domain.enums.EnvironmentParamCategory;
import com.example.demo.domain.repository.EnvironmentMeasurementRepository;
import com.example.demo.domain.repository.EnvironmentParameterRepository;
import com.example.demo.domain.repository.EnvironmentSensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnvironmentBroadcastScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final EnvironmentParameterRepository paramRepository;
    private final EnvironmentMeasurementRepository measurementRepository;
    private final EnvironmentSensorRepository sensorRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    // 5초마다 최신 환경 측정값 broadcast
    @Scheduled(fixedDelay = 5000)
    public void broadcastEnvironmentData() {
        List<EnvironmentParameter> params =
                paramRepository.findByParamCategory(EnvironmentParamCategory.MEASUREMENT);

        if (params.isEmpty()) return;

        List<Long> paramIds = params.stream().map(EnvironmentParameter::getParamId).toList();
        List<EnvironmentMeasurement> latestMeasurements =
                measurementRepository.findLatestByParamIds(paramIds);

        Map<Long, EnvironmentMeasurement> measurementMap = new HashMap<>();
        for (EnvironmentMeasurement m : latestMeasurements) {
            measurementMap.put(m.getParamId(), m);
        }

        Map<String, EnvironmentSensor> sensorMap = new HashMap<>();
        sensorRepository.findAll().forEach(s -> sensorMap.put(s.getSensorId(), s));

        for (EnvironmentParameter param : params) {
            EnvironmentMeasurement measurement = measurementMap.get(param.getParamId());
            if (measurement == null) continue;

            EnvironmentSensor sensor = sensorMap.get(param.getSensorId());
            if (sensor == null) continue;

            double value = measurement.getMeasuredValue();
            String status = determineStatus(value, param.getNormalMin(), param.getNormalMax());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "ENV_DATA");
            payload.put("sensorId", param.getSensorId());
            payload.put("zone", sensor.getZoneCode().name());
            payload.put("item", sensor.getSensorType().name());
            payload.put("tagCode", param.getTagCode());
            payload.put("value", value);
            payload.put("unit", param.getUnit());
            payload.put("normalMin", param.getNormalMin());
            payload.put("normalMax", param.getNormalMax());
            payload.put("status", status);
            payload.put("measuredAt", ZonedDateTime.of(measurement.getMeasuredAt(),
                    ZoneId.of("Asia/Seoul")).format(FORMATTER));

            messagingTemplate.convertAndSend("/topic/environment", payload);
        }
    }

    private String determineStatus(double value, Double min, Double max) {
        if (min == null || max == null) return "NORMAL";
        if (value < min || value > max) return "CRITICAL";
        double range = max - min;
        if (value < min + range * 0.05 || value > max - range * 0.05) return "WARNING";
        return "NORMAL";
    }
}
