package com.example.demo.domain.repository;

import com.example.demo.domain.entity.ProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessRepository extends JpaRepository<ProcessEntity, String> {
    List<ProcessEntity> findAllByOrderBySortOrderAsc();
}