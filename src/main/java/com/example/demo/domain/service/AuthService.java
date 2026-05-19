package com.example.demo.domain.service;

import com.example.demo.config.JwtUtil;
import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.AuthDto;
import com.example.demo.domain.entity.EmailVerification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.VerificationPurpose;
import com.example.demo.domain.repository.EmailVerificationRepository;
import com.example.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int EXPIRES_IN = 180; // 3분

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService; // ✅ 추가

    // refreshToken 임시 저장소 (추후 Redis로 교체 가능)
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    // ── 로그인 ────────────────────────────────────
    @Transactional
    public AuthDto.LoginResponse login(String userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ReportException(ErrorCode.UNAUTHORIZED,
                        "userId 없음 또는 비밀번호 불일치"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ReportException(ErrorCode.UNAUTHORIZED, "userId 없음 또는 비밀번호 불일치");
        }

        if (user.getStatus() != null) {
            switch (user.getStatus()) {
                case LOCKED   -> throw new ReportException(ErrorCode.FORBIDDEN, "잠금 처리된 계정입니다.");
                case INACTIVE -> throw new ReportException(ErrorCode.FORBIDDEN, "비활성화된 계정입니다.");
                default -> {}
            }
        }

        user.recordLogin();

        String accessToken  = jwtUtil.generateAccessToken(userId, user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        refreshTokenStore.put(userId, refreshToken);

        return AuthDto.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtUtil.getExpiresIn(accessToken))
                .userId(userId)
                .name(user.getName())
                .role(user.getRole().name())
                .roleLabel(roleLabel(user.getRole().name()))
                .build();
    }

    // ── 토큰 갱신 ─────────────────────────────────
    public AuthDto.RefreshResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new ReportException(ErrorCode.UNAUTHORIZED, "리프레시 토큰 만료 또는 위변조");
        }

        String userId = jwtUtil.getUserId(refreshToken);
        String stored = refreshTokenStore.get(userId);

        if (stored == null || !stored.equals(refreshToken)) {
            throw new ReportException(ErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ReportException(ErrorCode.UNAUTHORIZED, "사용자 없음"));

        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole().name());

        return AuthDto.RefreshResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtUtil.getExpiresIn(newAccessToken))
                .build();
    }

    // ── 로그아웃 ──────────────────────────────────
    public void logout(String userId) {
        refreshTokenStore.remove(userId);
    }

    // ── 이메일 인증코드 발송 (0.4) ────────────────
    @Transactional
    public AuthDto.EmailSendResponse sendEmailCode(String email, String purposeStr) {
        VerificationPurpose purpose = VerificationPurpose.valueOf(purposeStr);


        if (purpose == VerificationPurpose.FIND_ID || purpose == VerificationPurpose.FIND_PW) {
            userRepository.findByEmail(email)
                    .orElseThrow(() -> new ReportException(ErrorCode.BAD_REQUEST,
                            "해당 이메일로 가입된 계정 없음"));
        }

        // 기존 코드 삭제 후 새 코드 발급
        emailVerificationRepository.deleteByEmailAndPurpose(email, purpose);

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(EXPIRES_IN);

        emailVerificationRepository.save(EmailVerification.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .expiresAt(expiresAt)
                .build());

        // ✅ 실제 메일 발송
        emailService.sendVerificationCode(email, code, purposeStr);
        log.info("[이메일 인증코드 발송] email={}, purpose={}", email, purposeStr);

        return AuthDto.EmailSendResponse.builder()
                .email(email)
                .expiresIn(EXPIRES_IN)
                .build();
    }

    // ── 이메일 인증코드 확인 (0.5) ────────────────
    @Transactional
    public AuthDto.EmailVerifyResponse verifyEmailCode(String email, String code, String purposeStr) {
        VerificationPurpose purpose = VerificationPurpose.valueOf(purposeStr);

        EmailVerification ev = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new ReportException(ErrorCode.BAD_REQUEST,
                        "해당 이메일·purpose 발송 이력 없음"));

        if (ev.isExpired() || !ev.getCode().equals(code)) {
            throw new ReportException(ErrorCode.BAD_REQUEST, "코드 불일치 또는 만료");
        }

        // 인증 성공 후 코드 삭제
        emailVerificationRepository.deleteByEmailAndPurpose(email, purpose);

        // ✅ findAll() 제거 → findByEmail() 사용
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ReportException(ErrorCode.BAD_REQUEST, "사용자 없음"));

        if (purpose == VerificationPurpose.FIND_ID) {
            return AuthDto.EmailVerifyResponse.builder()
                    .userId(user.getUserId())
                    .build();

        } else {
            // ✅ 임시 비밀번호 생성 → DB 저장 → 메일 발송
            String tempPassword = generateTempPassword();
            user.updatePassword(passwordEncoder.encode(tempPassword));

            emailService.sendTemporaryPassword(email, tempPassword);
            log.info("[임시 비밀번호 발급] userId={}", user.getUserId());

            return AuthDto.EmailVerifyResponse.builder()
                    .userId(user.getUserId())
                    .temporaryPassword(tempPassword)
                    .resetAt(OffsetDateTime.now(KST).toString())
                    .build();
        }
    }

    // ── 헬퍼 ──────────────────────────────────────

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#";
        StringBuilder sb = new StringBuilder("Temp#");
        Random rand = new Random();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "ADMIN"    -> "관리자";
            case "LINE_MGR" -> "라인 관리자";
            case "WORKER"   -> "작업자";
            default         -> role;
        };
    }
}