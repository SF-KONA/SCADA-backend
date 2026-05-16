package com.example.demo.domain.controller;

import com.example.demo.domain.common.ApiResponse;
import com.example.demo.domain.dto.ProcessDto;
import com.example.demo.domain.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProcessDto.ProcessListResponse>> getProcessList() {
        return ResponseEntity.ok(ApiResponse.ok(processService.getProcessList()));
    }

    @GetMapping("/{stepNo}/partner")
    public ResponseEntity<ApiResponse<ProcessDto.PartnerResponse>> getPartner(
            @PathVariable String stepNo) {
        return ResponseEntity.ok(ApiResponse.ok(processService.getPartner(stepNo)));
    }
}