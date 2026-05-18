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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/environment")
@RequiredArgsConstructor
public class EnvironmentController {

    private static final int TREND_SIZE = 7;

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

        for (EnvironmentParameter param : params) {
            EnvironmentSensor sensor = sensorMap.get(param.getSensorId());
            if (sensor == null) continue;

            List<EnvironmentMeasurement> history =
                    measurementRepo.findTop7ByParamIdOrderByMeasuredAtDesc(param.getParamId());
            if (history.isEmpty()) continue;

            // 오래된 것이 앞에 오도록 역순 정렬
            Collections.reverse(history);

            List<Double> trend = history.stream()
                    .map(EnvironmentMeasurement::getMeasuredValue)
                    .collect(Collectors.toCollection(ArrayList::new));

            // DB에 7개 미만이면 가장 오래된 값으로 앞을 채움
            while (trend.size() < TREND_SIZE) {
                trend.add(0, trend.get(0));
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
