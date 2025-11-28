package com.programpractice.approval_request_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 결재 단계 응답 DTO
@Getter
@AllArgsConstructor
@Builder
class StepResponse {
    
    private Integer step;
    private Integer approverId;
    private String status;
    private LocalDateTime updatedAt;
    
    public static StepResponse from(com.programpractice.approval_request_service.document.Step step) {
        return StepResponse.builder()
                .step(step.getStep())
                .approverId(step.getApproverId())
                .status(step.getStatus())
                .updatedAt(step.getUpdatedAt())
                .build();
    }
}
