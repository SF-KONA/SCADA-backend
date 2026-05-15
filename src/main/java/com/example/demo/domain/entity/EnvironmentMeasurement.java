package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "environment_measurements",
        indexes = @Index(name = "idx_env_meas_param_at", columnList = "param_id, measured_at"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_id", nullable = false)
    private Long paramId;

    @Column(name = "measured_value", nullable = false)
    private Double measuredValue;

    @Column(name = "measured_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime measuredAt;
}