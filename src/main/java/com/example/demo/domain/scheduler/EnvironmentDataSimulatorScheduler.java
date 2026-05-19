package com.example.demo.domain.scheduler;

// 환경 측정값 시뮬레이터 - 10분마다 environment_measurements에 insert
import com.example.demo.domain.entity.EnvironmentMeasurement;
import com.example.demo.domain.entity.EnvironmentParameter;
import com.example.demo.domain.enums.EnvironmentParamCategory;
import com.example.demo.domain.repository.EnvironmentMeasurementRepository;
import com.example.demo.domain.repository.EnvironmentParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnvironmentDataSimulatorScheduler {

    private final EnvironmentParameterRepository paramRepository;
    private final EnvironmentMeasurementRepository measurementRepository;

    private final Random random = new Random();

    // 서버 시작 후 1초 뒤 실행 → 과거 데이터 없으면 채움
    @Scheduled(initialDelay = 1_000, fixedDelay = Long.MAX_VALUE)
    public void fillHistoricalData() {
        List<EnvironmentParameter> params =
                paramRepository.findByParamCategory(EnvironmentParamCategory.MEASUREMENT);
        if (params.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime from = now.minusMinutes(120);

        // 120분치 데이터가 이미 있으면 스킵
        long existingCount = measurementRepository
                .findByParamIdAndMeasuredAtAfterOrderByMeasuredAtAsc(
                        params.get(0).getParamId(), from)
                .size();
        if (existingCount >= 12) {
            log.info("[EnvSimulator] 과거 데이터 충분, 초기화 스킵");
            return;
        }

        List<EnvironmentMeasurement> measurements = new ArrayList<>();
        for (int i = 12; i >= 0; i--) {
            LocalDateTime slot = now.minusMinutes((long) i * 10);
            for (EnvironmentParameter param : params) {
                double min = param.getNormalMin() != null ? param.getNormalMin() : 0.0;
                double max = param.getNormalMax() != null ? param.getNormalMax() : 100.0;
                double range = max - min;
                double value = Math.round((min + range * 0.2 + range * 0.6 * random.nextDouble()) * 100.0) / 100.0;

                measurements.add(EnvironmentMeasurement.builder()
                        .paramId(param.getParamId())
                        .measuredValue(value)
                        .measuredAt(slot)
                        .build());
            }
        }
        measurementRepository.saveAll(measurements);
        log.info("[EnvSimulator] 과거 {}개 측정값 초기화 완료", measurements.size());
    }

    // 10분마다 현재 시각 데이터 insert
    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    public void insertSimulatedMeasurements() {
        List<EnvironmentParameter> params =
                paramRepository.findByParamCategory(EnvironmentParamCategory.MEASUREMENT);
        if (params.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<EnvironmentMeasurement> measurements = params.stream()
                .map(param -> {
                    double min = param.getNormalMin() != null ? param.getNormalMin() : 0.0;
                    double max = param.getNormalMax() != null ? param.getNormalMax() : 100.0;
                    double range = max - min;
                    double value = Math.round((min + range * 0.2 + range * 0.6 * random.nextDouble()) * 100.0) / 100.0;
                    return EnvironmentMeasurement.builder()
                            .paramId(param.getParamId())
                            .measuredValue(value)
                            .measuredAt(now)
                            .build();
                })
                .toList();

        measurementRepository.saveAll(measurements);
        log.info("[EnvSimulator] {}개 파라미터 측정값 insert 완료 at {}", measurements.size(), now);
    }
}