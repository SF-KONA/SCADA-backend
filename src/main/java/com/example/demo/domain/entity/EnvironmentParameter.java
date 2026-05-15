package com.example.demo.domain.entity;

import com.example.demo.domain.enums.DataType;
import com.example.demo.domain.enums.EnvironmentParamCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "environment_parameters")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id")
    private Long paramId;

    @Column(name = "sensor_id", nullable = false, length = 16)
    private String sensorId;  // FK -> environment_sensors.sensor_id

    @Column(name = "tag_code", nullable = false, length = 64)
    private String tagCode;  // 예: ENV_TEMP_01_VAL

    @Column(name = "tag_name", length = 64)
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
    private String collectionPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type")
    private DataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "param_category")
    private EnvironmentParamCategory paramCategory;
}