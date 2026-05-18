package com.example.demo.domain.repository;

import com.example.demo.domain.entity.EmailVerification;
import com.example.demo.domain.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 가장 최근 인증코드 조회
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email, VerificationPurpose purpose);

    // 동일 이메일+purpose 기존 코드 삭제 (재발송 시)
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.email = :email AND ev.purpose = :purpose")
    void deleteByEmailAndPurpose(@Param("email") String email,
                                 @Param("purpose") VerificationPurpose purpose);
}