package com.example.demo.domain.repository;

import com.example.demo.domain.entity.EquipmentParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipmentParameterRepository extends JpaRepository<EquipmentParameter, Long> {
    List<EquipmentParameter> findByEquipmentId(String equipmentId);
}