package com.programpractice.approval_request_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

// 승인 요청 메시지
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestMessage implements Serializable {
    
    private String approvalId;          // 승인 문서 ID (MongoDB ObjectId)
    private Long requesterId;           // 요청자 ID
    private String requesterName;       // 요청자 이름
    private String title;               // 제목
    private String content;             // 내용
    private LocalDateTime requestedAt;  // 요청 시간
}
