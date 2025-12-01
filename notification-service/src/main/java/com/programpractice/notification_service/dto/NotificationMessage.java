package com.programpractice.notification_service.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

// 클라이언트로 전송할 알림 메시지
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage implements Serializable {
    
    private Integer requestId;          // 승인 요청 ID
    private String result;              // "approved" or "rejected"
    private String finalResult;         // "approved" or "rejected" (최종 결과)
    private Integer rejectedBy;         // 반려한 직원 ID (반려 시에만)
    private LocalDateTime timestamp;    // 알림 시간
    
    // 추가 정보
    private String title;               // 승인 요청 제목
    private String message;             // 알림 메시지
}
