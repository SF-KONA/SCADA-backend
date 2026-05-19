package com.example.demo.domain.repository;

import com.example.demo.domain.entity.ManagerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ManagerNoteRepository extends JpaRepository<ManagerNote, Long> {

    List<ManagerNote> findByEquipmentIdOrderByCreatedAtDesc(String equipmentId);
}