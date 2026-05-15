package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "partners")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @Column(name = "step_no", length = 2)
    private String stepNo;  // 01=Wafer제조 / 08=패키징

    @Column(name = "product_name", nullable = false, length = 64)
    private String productName;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty;

    @Column(name = "stock_unit", nullable = false, length = 16)
    private String stockUnit;  // 장 / Box 등

    @Column(nullable = false)
    private LocalDate deadline;  // 납기일

    @Column(name = "company_name", nullable = false, length = 64)
    private String companyName;

    @Column(name = "contact_tel", length = 32)
    private String contactTel;

    @Column(name = "contact_email", length = 128)
    private String contactEmail;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "manager_name", nullable = false, length = 64)
    private String managerName;  // 담당자명

    @Column(name = "safety_stock")
    private Integer safetyStock;  // 안전 재고 기준 수량

    @Column(name = "delay_reason", length = 255)
    private String delayReason;  // 지연 사유
}