package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @Column(name = "equipment_id", length = 16)
    private String equipmentId;  // FURN_01 ~ PROBE_03

    @Column(name = "equipment_name", nullable = false, length = 64)
    private String equipmentName;

    @Column(name = "step_no", nullable = false, length = 2)
    private String stepNo;  // FK -> processes.step_no

    @Column(name = "unit_no", nullable = false)
    private Byte unitNo;  // 1 / 2 / 3

    @Column(name = "total_running_hours")
    private Integer totalRunningHours;  // 설비 가동시간
}