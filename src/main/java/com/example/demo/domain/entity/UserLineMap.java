package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_line_map")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLineMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 16)
    private String userId;  // LINE_MGR·WORKER만 사용

    @Column(name = "line_code", nullable = false, length = 32)
    private String lineCode;  // 담당 라인 코드

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}