package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "processes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessEntity {

    @Id
    @Column(name = "step_no", length = 2)
    private String stepNo;  // 01~08

    @Column(name = "process_name", nullable = false, length = 32)
    private String processName;

    @Column(name = "equipment_type", length = 64)
    private String equipmentType;

    @Column(name = "equipment_code", length = 8)
    private String equipmentCode;

    @Column(name = "sort_order", nullable = false)
    private Byte sortOrder;

    @Column(name = "has_equipment", nullable = false)
    @Builder.Default
    private Boolean hasEquipment = true;
}