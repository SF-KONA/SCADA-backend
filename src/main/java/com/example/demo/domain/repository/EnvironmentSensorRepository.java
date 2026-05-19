package com.example.demo.domain.repository;

import com.example.demo.domain.entity.EnvironmentSensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentSensorRepository extends JpaRepository<EnvironmentSensor, String> {
}
