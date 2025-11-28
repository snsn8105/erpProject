package com.programpractice.approval_request_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "approval_requests")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {
    
    @Id
    private String id;  // MongoDB _id
    
    private Integer requestId;  // 자동 증가 ID
    private Integer requesterId;
    private String title;
    private String content;
    private List<Step> steps;
    private String finalStatus;  // in_progress, approved, rejected
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    
    // 특정 단계의 상태 업데이트
    public void updateStepStatus(int stepNumber, String status) {
        this.updatedAt = LocalDateTime.now();
        steps.stream()
                .filter(s -> s.getStep() == stepNumber)
                .findFirst()
                .ifPresent(s -> s.updateStatus(status));
    }
    
    
    // 최종 상태 업데이트
    public void updateFinalStatus(String status) {
        this.finalStatus = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    
    // 다음 pending 단계 찾기
    public Step getNextPendingStep() {
        return steps.stream()
                .filter(s -> "pending".equals(s.getStatus()))
                .findFirst()
                .orElse(null);
    }
    
    
    // 모든 단계가 승인되었는지 확인
    public boolean areAllStepsApproved() {
        return steps.stream()
                .allMatch(s -> "approved".equals(s.getStatus()));
    }
}
