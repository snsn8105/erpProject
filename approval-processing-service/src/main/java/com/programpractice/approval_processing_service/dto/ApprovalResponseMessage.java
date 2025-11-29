package com.programpractice.approval_processing_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 승인 응답 메시지 (RabbitMQ Producer용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponseMessage implements Serializable {
    private String approvalId;           // MongoDB의 approvalId
    private Integer step;                // 처리된 단계
    private Long approverId;             // 승인자 ID
    private String approverName;         // 승인자 이름
    private String status;               // 단계 상태 (approved, rejected, pending)
    private String finalStatus;          // 최종 상태 (approved, rejected, pending)
    private String comment;              // 코멘트
    private LocalDateTime processedAt;   // 처리 시간
    private boolean success;             // 처리 성공 여부
    private String errorMessage;         // 에러 메시지
}