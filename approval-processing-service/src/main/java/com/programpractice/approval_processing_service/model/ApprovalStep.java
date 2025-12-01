package com.programpractice.approval_processing_service.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalStep {
    
    private Long id; // 자동 증가 ID
    private Integer step; // 단계 번호
    private Long approverId; // 승인자 ID

    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private String comment; // 승인, 반려 코멘트
    private LocalDateTime processedAt;

    // 승인 처리
    public void approve(String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }

    // 반려 처리
    public void reject(String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();        
    }
}
