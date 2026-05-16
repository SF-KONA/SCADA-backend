package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, String> {
    List<Equipment> findByStepNo(String stepNo);
    int countByStepNo(String stepNo);
}