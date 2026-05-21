package com.example.demo.domain.controller;

import com.example.demo.domain.entity.EnvironmentMeasurement;
import com.example.demo.domain.entity.EnvironmentParameter;
import com.example.demo.domain.entity.EnvironmentSensor;
import com.example.demo.domain.enums.EnvironmentParamCategory;
import com.example.demo.domain.repository.EnvironmentMeasurementRepository;
import com.example.demo.domain.repository.EnvironmentParameterRepository;
import com.example.demo.domain.repository.EnvironmentSensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/environment")
@RequiredArgsConstructor
public class EnvironmentController {

    private static final int TREND_SIZE = 13;

    private final EnvironmentParameterRepository paramRepo;
    private final EnvironmentMeasurementRepository measurementRepo;
    private final EnvironmentSensorRepository sensorRepo;

    @GetMapping("/history")
    public List<Map<String, Object>> getHistory() {
        List<EnvironmentParameter> params =
                paramRepo.findByParamCategory(EnvironmentParamCategory.MEASUREMENT);

        Map<String, EnvironmentSensor> sensorMap = sensorRepo.findAll().stream()
                .collect(Collectors.toMap(EnvironmentSensor::getSensorId, s -> s));

        List<Map<String, Object>> result = new ArrayList<>();

        // ★ 현재 시각 기준
        LocalDateTime now = LocalDateTime.now();

        for (EnvironmentParameter param : params) {
            EnvironmentSensor sensor = sensorMap.get(param.getSensorId());
            if (sensor == null) continue;

            // ★ 현재 시각 이전 데이터만 조회
            List<EnvironmentMeasurement> history =
                    measurementRepo.findTop13ByParamIdBeforeNow(param.getParamId(), now);
            if (history.isEmpty()) continue;

            Collections.reverse(history);

            List<Double> trend = history.stream()
                    .map(EnvironmentMeasurement::getMeasuredValue)
                    .collect(Collectors.toCollection(ArrayList::new));

            List<String> times = history.stream()
                    .map(h -> h.getMeasuredAt()
                            .atZone(java.time.ZoneId.of("Asia/Seoul"))
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                    .collect(Collectors.toCollection(ArrayList::new));

            while (trend.size() < TREND_SIZE) {
                trend.add(0, trend.get(0));
                times.add(0, times.get(0));
            }

            double currentValue = trend.get(trend.size() - 1);
            String status = determineStatus(currentValue, param.getNormalMin(), param.getNormalMax());

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("item", sensor.getSensorType().name());
            entry.put("zone", sensor.getZoneCode().name());
            entry.put("value", currentValue);
            entry.put("status", status);
            entry.put("normalMin", param.getNormalMin());
            entry.put("normalMax", param.getNormalMax());
            entry.put("trend", trend);
            entry.put("times", times);
            result.add(entry);
        }

        return result;
    }

    private String determineStatus(double value, Double min, Double max) {
        if (min == null || max == null) return "NORMAL";
        if (value < min || value > max) return "CRITICAL";
        double range = max - min;
        if (value < min + range * 0.05 || value > max - range * 0.05) return "WARNING";
        return "NORMAL";
    }
}