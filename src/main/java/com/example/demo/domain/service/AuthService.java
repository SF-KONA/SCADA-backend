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

        // 해당 이메일로 가입된 계정 있는지 확인
        if (purpose == VerificationPurpose.FIND_ID || purpose == VerificationPurpose.FIND_PW) {
            userRepository.findAll().stream()
                    .filter(u -> u.getEmail().equals(email))
                    .findFirst()
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

        // 테스트용: 콘솔 출력 (추후 실제 이메일 발송으로 교체)
        log.info("====================================");
        log.info("[이메일 인증코드] {} / purpose={} / code={}", email, purposeStr, code);
        log.info("====================================");

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

        // 해당 이메일의 사용자 조회
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new ReportException(ErrorCode.BAD_REQUEST, "사용자 없음"));

        if (purpose == VerificationPurpose.FIND_ID) {
            return AuthDto.EmailVerifyResponse.builder()
                    .userId(user.getUserId())
                    .build();
        } else {
            // FIND_PW: 임시 비밀번호 발급
            String tempPassword = generateTempPassword();
            String resetAt = OffsetDateTime.now(KST).toString();

            // 실제로는 비밀번호 업데이트 필요 (현재는 콘솔 출력)
            log.info("====================================");
            log.info("[임시 비밀번호] userId={} / tempPassword={}", user.getUserId(), tempPassword);
            log.info("====================================");

            return AuthDto.EmailVerifyResponse.builder()
                    .userId(user.getUserId())
                    .temporaryPassword(tempPassword)
                    .resetAt(resetAt)
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