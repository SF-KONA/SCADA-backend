package com.example.demo.domain.repository;

import com.example.demo.domain.entity.PmSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PmScheduleRepository extends JpaRepository<PmSchedule, Long> {
    List<PmSchedule> findByEquipmentId(String equipmentId);
}