package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_notes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "user_id", nullable = false, length = 16)
    private String userId;

    @Column(name = "equipment_id", nullable = false, length = 16)
    private String equipmentId;

    @Column(name = "note_text", nullable = false, length = 500)
    private String noteText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
