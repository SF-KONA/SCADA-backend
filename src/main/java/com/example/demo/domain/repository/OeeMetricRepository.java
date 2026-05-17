package com.example.demo.domain.repository;

import com.example.demo.domain.entity.OeeMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OeeMetricRepository extends JpaRepository<OeeMetric, Long> {

    @Query("SELECT o FROM OeeMetric o WHERE o.equipmentId = :equipmentId ORDER BY o.periodStart DESC LIMIT 1")
    Optional<OeeMetric> findLatestByEquipmentId(@Param("equipmentId") String equipmentId);
}
