package com.programpractice.approval_processing_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * REST API: 승인/반려 처리 요청 (Request Body)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessApprovalRequest {
    private String status;       // "approved" or "rejected"
    private String comment;      // 코멘트
}
