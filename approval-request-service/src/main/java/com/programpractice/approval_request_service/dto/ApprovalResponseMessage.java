package com.programpractice.approval_request_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

// 승인 응답 메시지
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponseMessage implements Serializable {
    
    private String approvalId;          // 승인 문서 ID
    private String status;              // 처리 상태 (APPROVED, REJECTED, PENDING)
    private Long approverId;            // 승인자 ID (있을 경우)
    private String approverName;        // 승인자 이름
    private String comment;             // 코멘트
    private LocalDateTime processedAt;  // 처리 시간
    private boolean success;            // 처리 성공 여부
    private String errorMessage;        // 에러 메시지 (실패 시)
}
