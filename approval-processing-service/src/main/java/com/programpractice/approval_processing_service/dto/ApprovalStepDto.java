package com.programpractice.approval_processing_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 승인 단계 상세 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalStepDto {
    private Integer step;
    private Long approverId;
    private String status;
    private String comment;
    private LocalDateTime processedAt;
}
