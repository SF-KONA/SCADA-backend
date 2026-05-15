package com.example.demo.domain.entity;

import com.example.demo.domain.enums.SensorType;
import com.example.demo.domain.enums.ZoneCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "environment_sensors")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentSensor {

    @Id
    @Column(name = "sensor_id", length = 16)
    private String sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false)
    private SensorType sensorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_code", nullable = false)
    private ZoneCode zoneCode;

    @Column(name = "sensor_name", length = 64)
    private String sensorName;
}