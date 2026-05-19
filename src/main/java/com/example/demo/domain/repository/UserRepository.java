package com.example.demo.domain.repository;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.UserRole;
import com.example.demo.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserId(String userId);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);  // ✅ 추가

    @Query("""
        SELECT u FROM User u
        WHERE (:q IS NULL OR u.userId LIKE %:q% OR u.name LIKE %:q% OR u.email LIKE %:q%)
          AND (:factoryCode IS NULL OR u.factoryCode = :factoryCode)
          AND (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
        ORDER BY u.createdAt DESC
    """)
    List<User> findAllFiltered(
            @Param("q") String q,
            @Param("factoryCode") String factoryCode,
            @Param("role") UserRole role,
            @Param("status") UserStatus status
    );
}