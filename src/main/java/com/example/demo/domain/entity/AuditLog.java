package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 16)
    private String userId;  // 비로그인 이벤트는 NULL

    @Column(nullable = false, length = 64)
    private String action;  // LOGIN_SUCCESS / LOGIN_FAIL / REGISTER / PW_CHANGE / LOCK 등

    @Column(name = "target_type", length = 32)
    private String targetType;  // USER, ALARM, SUGGESTION 등

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(columnDefinition = "JSON")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}