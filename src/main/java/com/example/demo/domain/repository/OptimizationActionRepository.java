package com.example.demo.domain.repository;

import com.example.demo.domain.entity.OptimizationAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptimizationActionRepository extends JpaRepository<OptimizationAction, Long> {
}
