package com.programpractice.approval_processing_service.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
public class ApprovalRequest {
    
    private String id; // MongoDB의 approvalId와 매핑
    private Integer requestId;  
    private Long requesterId;
    private String title;
    private String content;
    
    @Builder.Default
    private List<ApprovalStep> steps = new ArrayList<>();
    
    @Builder.Default
    private ApprovalStatus finalStatus = ApprovalStatus.PENDING;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 승인 단계 추가
    public void addStep(ApprovalStep step) {
        steps.add(step);
    }

    // 최종 상태 업데이트
    public void updateFinalStatus(ApprovalStatus status) {
        this.finalStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    // 다음 PENDING 단계 찾기
    public ApprovalStep getNextPendingStep() {
        return steps.stream()
                .filter(step -> step.getStatus() == ApprovalStatus.PENDING)
                .findFirst()
                .orElse(null);
    }
}
