package com.example.demo.domain.service;

import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.entity.AuditLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserLineMap;
import com.example.demo.domain.enums.UserRole;
import com.example.demo.domain.enums.UserStatus;
import com.example.demo.domain.repository.AuditLogRepository;
import com.example.demo.domain.repository.UserLineMapRepository;
import com.example.demo.domain.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final Map<String, String> FACTORY_NAMES = Map.of(
            "ICN", "인천",
            "PTK", "평택"
    );

    private static final Map<UserRole, String> ROLE_LABELS = Map.of(
            UserRole.ADMIN,    "관리자",
            UserRole.LINE_MGR, "라인장",
            UserRole.WORKER,   "작업자"
    );

    private static final Map<String, String> ACTION_LABELS = Map.of(
            "REGISTER",    "사용자 등록",
            "INFO_UPDATE", "사용자 정보 수정",
            "PW_CHANGE",   "비밀번호 초기화",
            "LINE_UPDATE", "담당 라인 수정"
    );

    private final UserRepository userRepository;
    private final UserLineMapRepository lineMapRepository;
    private final AuditLogRepository auditLogRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // ── 목록 조회 ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserDto.ListResponse getUsers(String keyword, String factoryCode, String roleStr, String statusStr) {
        UserRole role     = parseEnum(roleStr,   UserRole.class);
        UserStatus status = parseEnum(statusStr, UserStatus.class);
        String q  = blankToNull(keyword);
        String fc = blankToNull(factoryCode);

        List<User> users = userRepository.findAllFiltered(q, fc, role, status);
        List<UserDto.UserItem> items = users.stream().map(this::toItem).toList();
        return new UserDto.ListResponse(items, items.size());
    }

    // ── 상세 조회 ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserDto.UserDetailResponse getUserDetail(String userId) {
        User user = findUser(userId);
        List<UserDto.LineItem> lines = toLineItems(lineMapRepository.findByUserId(userId));
        return toDetailResponse(user, lines);
    }

    // ── 등록 ─────────────────────────────────────────────────────────────────
    @Transactional
    public UserDto.RegisterResponse createUser(UserDto.CreateRequest req, String performedBy) {
        if (userRepository.existsById(req.getUserId())) {
            throw new ReportException(ErrorCode.DUPLICATE_USER_ID);
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ReportException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .userId(req.getUserId())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .email(req.getEmail())
                .factoryCode(req.getFactoryCode())
                .role(req.getRole())
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        writeAudit(performedBy, "REGISTER", "USER", req.getUserId(), Map.of());
        return new UserDto.RegisterResponse(user.getUserId(), user.getRole().name());
    }

    // ── 정보 수정 (부분 업데이트) ────────────────────────────────────────────
    @Transactional
    public UserDto.UserItem updateUser(String userId, UserDto.UpdateRequest req, String performedBy) {
        User user = findUser(userId);

        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(req.getEmail())) {
            throw new ReportException(ErrorCode.DUPLICATE_EMAIL);
        }

        Map<String, Object> before = Map.of(
                "name",        user.getName(),
                "email",       user.getEmail(),
                "factoryCode", user.getFactoryCode() != null ? user.getFactoryCode() : "",
                "role",        user.getRole().name()
        );

        String newName    = req.getName()        != null ? req.getName()        : user.getName();
        String newEmail   = req.getEmail()       != null ? req.getEmail()       : user.getEmail();
        String newFactory = req.getFactoryCode() != null ? req.getFactoryCode() : user.getFactoryCode();
        UserRole newRole  = req.getRole()        != null ? req.getRole()        : user.getRole();

        user.updateInfo(newName, newEmail, newFactory, newRole);

        Map<String, Object> after = Map.of(
                "name",        newName,
                "email",       newEmail,
                "factoryCode", newFactory != null ? newFactory : "",
                "role",        newRole.name()
        );

        writeAudit(performedBy, "INFO_UPDATE", "USER", userId, Map.of("before", before, "after", after));
        return toItem(user);
    }

    // ── 비밀번호 초기화 ──────────────────────────────────────────────────────
    @Transactional
    public UserDto.ResetPasswordResponse resetPassword(String userId, String performedBy) {
        User user = findUser(userId);
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "!1A";
        user.updatePassword(passwordEncoder.encode(tempPassword));
        writeAudit(performedBy, "PW_CHANGE", "USER", userId, Map.of());
        return new UserDto.ResetPasswordResponse(userId, tempPassword, OffsetDateTime.now(KST));
    }

    // ── 계정 상태 변경 ───────────────────────────────────────────────────────
    @Transactional
    public void lockUser(String userId) { findUser(userId).updateStatus(UserStatus.LOCKED); }

    @Transactional
    public void unlockUser(String userId) { findUser(userId).updateStatus(UserStatus.ACTIVE); }

    @Transactional
    public void deactivateUser(String userId) { findUser(userId).updateStatus(UserStatus.INACTIVE); }

    @Transactional
    public void activateUser(String userId) { findUser(userId).updateStatus(UserStatus.ACTIVE); }

    // ── 담당 라인 조회 ───────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserDto.LinesResponse getUserLines(String userId) {
        findUser(userId);   // 404 체크
        List<UserDto.LineItem> lines = toLineItems(lineMapRepository.findByUserId(userId));
        return new UserDto.LinesResponse(userId, lines);
    }

    // ── 담당 라인 수정 (일괄 교체) ───────────────────────────────────────────
    @Transactional
    public UserDto.LinesResponse updateUserLines(String userId, List<String> lineCodes, String performedBy) {
        findUser(userId);   // 404 체크

        List<String> before = lineMapRepository.findByUserId(userId)
                .stream().map(UserLineMap::getLineCode).toList();

        lineMapRepository.deleteByUserId(userId);

        List<UserLineMap> newMaps = lineCodes.stream()
                .map(code -> UserLineMap.builder()
                        .userId(userId)
                        .lineCode(code)
                        .build())
                .toList();
        lineMapRepository.saveAll(newMaps);

        writeAudit(performedBy, "LINE_UPDATE", "USER", userId,
                Map.of("before", before, "after", lineCodes));

        List<UserDto.LineItem> lines = toLineItems(lineMapRepository.findByUserId(userId));
        return new UserDto.LinesResponse(userId, lines);
    }

    // ── 변경 이력 조회 ───────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserDto.AuditResponse getAuditLogs(String userId, int page, int size) {
        findUser(userId);   // 404 체크

        Page<AuditLog> pageResult = auditLogRepository
                .findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
                        "USER", userId, PageRequest.of(page, size));

        List<UserDto.AuditLogItem> logs = pageResult.getContent().stream()
                .map(this::toAuditLogItem)
                .toList();

        return new UserDto.AuditResponse(userId, (int) pageResult.getTotalElements(), page, size, logs);
    }

    // ── 중복 확인 ────────────────────────────────────────────────────────────
    public boolean checkUserIdAvailable(String userId) {
        return !userRepository.existsById(userId);
    }

    public boolean checkEmailAvailable(String email, String excludeUserId) {
        if (excludeUserId != null && !excludeUserId.isBlank()) {
            return userRepository.findAll().stream()
                    .filter(u -> !u.getUserId().equals(excludeUserId))
                    .noneMatch(u -> u.getEmail().equals(email));
        }
        return !userRepository.existsByEmail(email);
    }

    // ── 공장 목록 ────────────────────────────────────────────────────────────
    public List<UserDto.FactoryItem> getFactories() {
        return FACTORY_NAMES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new UserDto.FactoryItem(e.getKey(), e.getValue()))
                .toList();
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────
    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ReportException(ErrorCode.USER_NOT_FOUND));
    }

    private void writeAudit(String performedBy, String action, String targetType,
                            String targetId, Object detail) {
        try {
            String detailJson = objectMapper.writeValueAsString(detail);
            auditLogRepository.save(AuditLog.builder()
                    .userId(performedBy)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detailJson)
                    .occurredAt(LocalDateTime.now(KST))
                    .build());
        } catch (Exception e) {
            log.warn("감사 로그 기록 실패: {}", e.getMessage());
        }
    }

    private UserDto.UserItem toItem(User u) {
        UserStatus status = u.getStatus() != null ? u.getStatus() : UserStatus.ACTIVE;
        String factoryCode = u.getFactoryCode() != null ? u.getFactoryCode() : "";
        return new UserDto.UserItem(
                u.getUserId(), u.getName(), u.getEmail(),
                factoryCode, FACTORY_NAMES.getOrDefault(factoryCode, factoryCode),
                u.getRole().name(), ROLE_LABELS.getOrDefault(u.getRole(), u.getRole().name()),
                status.name(),
                toKst(u.getLastLoginAt()),
                toKst(u.getCreatedAt())
        );
    }

    private UserDto.UserDetailResponse toDetailResponse(User u, List<UserDto.LineItem> lines) {
        UserStatus status = u.getStatus() != null ? u.getStatus() : UserStatus.ACTIVE;
        String factoryCode = u.getFactoryCode() != null ? u.getFactoryCode() : "";
        return new UserDto.UserDetailResponse(
                u.getUserId(), u.getName(), u.getEmail(),
                factoryCode, FACTORY_NAMES.getOrDefault(factoryCode, factoryCode),
                u.getRole().name(), ROLE_LABELS.getOrDefault(u.getRole(), u.getRole().name()),
                status.name(),
                toKst(u.getLastLoginAt()),
                toKst(u.getCreatedAt()),
                lines
        );
    }

    private List<UserDto.LineItem> toLineItems(List<UserLineMap> maps) {
        return maps.stream()
                .map(m -> new UserDto.LineItem(m.getLineCode(), toKst(m.getCreatedAt())))
                .toList();
    }

    private UserDto.AuditLogItem toAuditLogItem(AuditLog log) {
        Object detailObj = Map.of();
        try {
            if (log.getDetail() != null && !log.getDetail().isBlank()) {
                detailObj = objectMapper.readValue(log.getDetail(), new TypeReference<Object>() {});
            }
        } catch (Exception e) {
            detailObj = Map.of();
        }
        return new UserDto.AuditLogItem(
                log.getId(),
                log.getAction(),
                ACTION_LABELS.getOrDefault(log.getAction(), log.getAction()),
                log.getUserId(),
                detailObj,
                toKst(log.getOccurredAt())
        );
    }

    private OffsetDateTime toKst(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(KST).toOffsetDateTime();
    }

    private <T extends Enum<T>> T parseEnum(String val, Class<T> clazz) {
        if (val == null || val.isBlank()) return null;
        try { return Enum.valueOf(clazz, val); }
        catch (IllegalArgumentException e) { return null; }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
