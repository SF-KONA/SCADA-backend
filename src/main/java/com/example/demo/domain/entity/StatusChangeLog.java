package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_change_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false, length = 16)
    private String equipmentId;

    @Column(name = "prev_status")
    private Byte prevStatus;  // 0=idle / 1=run / 2=alarm / 3=PM

    @Column(name = "new_status")
    private Byte newStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}