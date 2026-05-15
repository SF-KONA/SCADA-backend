package com.example.demo.domain.entity;

import com.example.demo.domain.enums.DataType;
import com.example.demo.domain.enums.EquipmentParamCategory;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipment_parameters")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id")
    private Long paramId;

    @Column(name = "equipment_id", nullable = false, length = 16)
    private String equipmentId;  // FK -> equipments.equipment_id

    @Column(name = "tag_code", nullable = false, length = 64)
    private String tagCode;  // 예: FURN_01_TEMP

    @Column(name = "tag_name", nullable = false, length = 64)
    private String tagName;

    @Column(length = 16)
    private String unit;

    @Column(name = "normal_min")
    private Double normalMin;

    @Column(name = "normal_max")
    private Double normalMax;

    @Column(name = "abnormal_condition", length = 255)
    private String abnormalCondition;

    @Column(name = "collection_period", length = 8)
    private String collectionPeriod;  // 1s / 5s / 1m / lot

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type")
    private DataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "param_category")
    private EquipmentParamCategory paramCategory;

    @Column(name = "is_controllable", nullable = false)
    @Builder.Default
    private Boolean isControllable = false;  // AI 제어 대상 여부
}
