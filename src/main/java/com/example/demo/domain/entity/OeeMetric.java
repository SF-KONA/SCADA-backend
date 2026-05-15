package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oee_metrics",
       indexes = @Index(name = "idx_oee_eq_period", columnList = "equipment_id, period_start"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OeeMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false, length = 16)
    private String equipmentId;  // FK -> equipments

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;  // 집계 구간 시작 시각

    @Column
    private Double availability;  // 가용률 = 누적시간 / 계획가동시간

    @Column
    private Double performance;  // 성능률 = (이상CT × PROD_COUNT) / 실제가동시간

    @Column
    private Double quality;  // 품질률 = (PROD_COUNT - NG_COUNT) / PROD_COUNT

    @Column
    private Double oee;  // availability × performance × quality

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
