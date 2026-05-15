package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_actions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "suggestion_id", nullable = false)
    private Long suggestionId;  // FK -> ai_suggestions

    @Column(name = "equipment_id", nullable = false, length = 16)
    private String equipmentId;

    @Column(name = "parameter_tag", nullable = false, length = 64)
    private String parameterTag;

    @Column(name = "before_value")
    private Double beforeValue;  // 액션 직전 값

    @Column(name = "after_value")
    private Double afterValue;  // REJECT면 before_value와 동일

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "user_id", nullable = false, length = 16)
    private String userId;  // 액션 수행자 (LINE_MGR·ADMIN만)

    @Column(length = 200)
    private String comment;  // 액션 사유 코멘트

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;
}
