package com.programpractice.approval_request_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 승인 응답 메시지 (RabbitMQ용)
 * Processing Service의 ApprovalResponseMessage와 필드명 일치 필요
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponseMessage implements Serializable {
    
    private String requestId;            // MongoDB ObjectId
    private Integer step;                // 처리된 단계
    private Long approverId;             // 승인자 ID
    private String approverName;         // 승인자 이름
    private String status;               // 단계 상태 (approved, rejected, pending)
    private String finalStatus;          // 최종 상태 (approved, rejected, pending)
    private String comment;              // 코멘트
    private LocalDateTime updatedAt;     // 업데이트 시간
    private LocalDateTime processedAt;   // 처리 시간
    private boolean success;             // 처리 성공 여부
    private String errorMessage;         // 에러 메시지

    // 추가 정보
    private Integer numericRequestId;    // 숫자 형태의 요청 ID
    private Integer requesterId;         // 요청자 ID
    private String title;                // 승인 요청 제목
}
