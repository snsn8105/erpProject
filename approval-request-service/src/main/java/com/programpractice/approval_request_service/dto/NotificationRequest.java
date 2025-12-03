package com.programpractice.approval_request_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Notification Service로 전송할 알림 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    
    private Integer requestId;      // 승인 요청 ID
    private Integer requesterId;    // 요청자 ID (알림 받을 사람)
    private String title;           // 승인 요청 제목
    private String finalStatus;     // 최종 상태 (approved, rejected)
    private Integer rejectedBy;     // 반려한 승인자 ID (반려 시에만)
}
