package com.example.demo.domain.repository;

import com.example.demo.domain.entity.UserLineMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLineMapRepository extends JpaRepository<UserLineMap, Long> {
    List<UserLineMap> findByUserId(String userId);
    void deleteByUserId(String userId);
}
