package com.example.demo.domain.service;

import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.SuggestionDto;
import com.example.demo.domain.entity.AuditLog;
import com.example.demo.domain.entity.Equipment;
import com.example.demo.domain.entity.EquipmentMeasurement;
import com.example.demo.domain.entity.EquipmentParameter;
import com.example.demo.domain.repository.AuditLogRepository;
import com.example.demo.domain.repository.EquipmentMeasurementRepository;
import com.example.demo.domain.repository.EquipmentParameterRepository;
import com.example.demo.domain.repository.EquipmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParameterControlService {

    private final EquipmentRepository            equipmentRepo;
    private final EquipmentParameterRepository   parameterRepo;
    private final EquipmentMeasurementRepository measurementRepo;
    private final AuditLogRepository             auditLogRepo;
    private final ObjectMapper                   objectMapper;

    // ─── 5.5 제어 가능 파라미터 목록 ─────────────────────
    @Transactional(readOnly = true)
    public SuggestionDto.ControllableListResponse listControllable(String equipmentId) {
        Equipment equipment = equipmentRepo.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        List<SuggestionDto.ControllableItem> items = parameterRepo.findByEquipmentId(equipmentId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsControllable()))
                .map(p -> {
                    Double currentValue = measurementRepo.findLatestByParamId(p.getParamId())
                            .map(EquipmentMeasurement::getMeasuredValue)
                            .orElse(null);
                    return SuggestionDto.ControllableItem.builder()
                            .paramId(p.getParamId())
                            .tagCode(p.getTagCode())
                            .tagName(p.getTagName())
                            .unit(p.getUnit())
                            .normalMin(p.getNormalMin())
                            .normalMax(p.getNormalMax())
                            .currentValue(currentValue)
                            .dataType(p.getDataType() != null ? p.getDataType().name() : null)
                            .paramCategory(p.getParamCategory() != null ? p.getParamCategory().name() : null)
                            .build();
                })
                .toList();

        return SuggestionDto.ControllableListResponse.builder()
                .equipmentId(equipment.getEquipmentId())
                .equipmentName(equipment.getEquipmentName())
                .items(items)
                .build();
    }

    // ─── 5.6 파라미터 수동 변경 ─────────────────────────
    @Transactional
    public SuggestionDto.ParamUpdateResponse updateParameter(String equipmentId,
                                                             Long paramId,
                                                             Double newValue,
                                                             String comment,
                                                             String userId) {
        if (newValue == null) {
            throw new ReportException(ErrorCode.VALIDATION_ERROR);
        }
        if (comment != null && comment.length() > 200) {
            throw new ReportException(ErrorCode.VALIDATION_ERROR);
        }

        equipmentRepo.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        EquipmentParameter param = parameterRepo.findById(paramId)
                .orElseThrow(() -> new ReportException(ErrorCode.PARAMETER_NOT_FOUND));

        if (!equipmentId.equals(param.getEquipmentId())) {
            throw new ReportException(ErrorCode.PARAMETER_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(param.getIsControllable())) {
            throw new ReportException(ErrorCode.NOT_CONTROLLABLE);
        }

        Double beforeValue = measurementRepo.findLatestByParamId(paramId)
                .map(EquipmentMeasurement::getMeasuredValue)
                .orElse(null);

        LocalDateTime changedAt = LocalDateTime.now();

        // 1) equipment_measurements 에 새 측정값 INSERT
        //    (실제 PLC 갱신을 시뮬레이션. 다음 GET 호출 시 변경된 값이 즉시 반영됨)
        EquipmentMeasurement measurement = EquipmentMeasurement.builder()
                .paramId(paramId)
                .measuredValue(newValue)
                .measuredAt(changedAt)
                .build();
        measurementRepo.save(measurement);

        // 2) audit_logs 에 변경 이력 INSERT
        //    실패해도 본 흐름은 진행 (UserService 패턴 동일)
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("equipmentId", equipmentId);
            detail.put("tagCode",     param.getTagCode());
            detail.put("before",      beforeValue);
            detail.put("after",       newValue);
            detail.put("comment",     comment);

            auditLogRepo.save(AuditLog.builder()
                    .userId(userId)
                    .action("PARAM_CHANGE")
                    .targetType("EQUIPMENT_PARAMETER")
                    .targetId(String.valueOf(paramId))
                    .detail(objectMapper.writeValueAsString(detail))
                    .occurredAt(changedAt)
                    .build());
        } catch (Exception e) {
            log.warn("PARAM_CHANGE 감사 로그 기록 실패: {}", e.getMessage());
        }

        return SuggestionDto.ParamUpdateResponse.builder()
                .equipmentId(equipmentId)
                .paramId(param.getParamId())
                .tagCode(param.getTagCode())
                .tagName(param.getTagName())
                .unit(param.getUnit())
                .beforeValue(beforeValue)
                .afterValue(newValue)
                .userId(userId)
                .comment(comment)
                .changedAt(changedAt)
                .build();
    }
}
