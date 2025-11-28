package com.programpractice.approval_request_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 결재 요청 생성 응답 DTO
@Getter
@AllArgsConstructor
public class ApprovalCreateResponse {
    private Integer requestId;
}
