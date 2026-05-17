package com.example.demo.domain.service;

import com.example.demo.domain.common.ErrorCode;
import com.example.demo.domain.common.ReportException;
import com.example.demo.domain.dto.SuggestionDto;
import com.example.demo.domain.entity.Equipment;
import com.example.demo.domain.entity.EquipmentMeasurement;
import com.example.demo.domain.entity.EquipmentParameter;
import com.example.demo.domain.repository.EquipmentMeasurementRepository;
import com.example.demo.domain.repository.EquipmentParameterRepository;
import com.example.demo.domain.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParameterControlService {

    private final EquipmentRepository           equipmentRepo;
    private final EquipmentParameterRepository  parameterRepo;
    private final EquipmentMeasurementRepository measurementRepo;

    // ─── 5.5 제어 가능 파라미터 목록 ─────────────────────
    @Transactional(readOnly = true)
    public SuggestionDto.ControllableListResponse listControllable(String equipmentId) {
        Equipment equipment = equipmentRepo.findById(equipmentId)
                .orElseThrow(() -> new ReportException(ErrorCode.EQUIPMENT_NOT_FOUND));

        List<SuggestionDto.ControllableItem> items = parameterRepo.findByEquipmentId(equipmentId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsControllable()))
                .map(p -> SuggestionDto.ControllableItem.builder()
                        .paramId(p.getParamId())
                        .tagCode(p.getTagCode())
                        .tagName(p.getTagName())
                        .unit(p.getUnit())
                        .normalMin(p.getNormalMin())
                        .normalMax(p.getNormalMax())
                        .dataType(p.getDataType() != null ? p.getDataType().name() : null)
                        .paramCategory(p.getParamCategory() != null ? p.getParamCategory().name() : null)
                        .build())
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

        // 명세: 실제 측정값 INSERT 없이 응답만. 액션은 OptimizationAction이 아니라 단순 변경 로그 응답.
        // (audit_logs INSERT 는 인증 통합 후 추가 예정)

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
                .changedAt(LocalDateTime.now())
                .build();
    }
}
