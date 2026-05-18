package com.example.demo.domain.repository;

import com.example.demo.domain.entity.OptimizationAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptimizationActionRepository extends JpaRepository<OptimizationAction, Long> {

    List<OptimizationAction> findByEquipmentIdOrderByActedAtDesc(String equipmentId, Pageable pageable);
}
