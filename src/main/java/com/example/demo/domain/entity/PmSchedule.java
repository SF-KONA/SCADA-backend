package com.example.demo.domain.entity;

import com.example.demo.domain.enums.PmType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pm_schedules")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false, length = 16)
    private String equipmentId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "pm_type", nullable = false)
    private PmType pmType;
}