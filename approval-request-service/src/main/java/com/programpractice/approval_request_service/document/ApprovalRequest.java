// approval-request-service/src/main/java/com/programpractice/approval_request_service/document/ApprovalRequest.java
package com.programpractice.approval_request_service.document;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    
    @Builder.Default
    private Integer currentStepOrder = 1;  // 현재 진행 중인 단계 (1부터 시작)
    
    private String finalStatus;  // in_progress, approved, rejected
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    
    // 현재 단계 가져오기 (O(1) 접근)
    public Step getCurrentStep() {
        if (!"in_progress".equals(this.finalStatus)) {
            return null; // 이미 끝나거나 반려된 건
        }
        
        int index = this.currentStepOrder - 1; // 리스트 인덱스는 0부터 시작
        
        if (index >= 0 && index < steps.size()) {
            return steps.get(index);
        }
        return null;
    }
    
    // 다음 단계로 이동
    public void moveToNextStep() {
        this.currentStepOrder++;
        this.updatedAt = LocalDateTime.now();
    }
    
    // 마지막 단계인지 확인
    public boolean isLastStep() {
        return this.currentStepOrder >= steps.size();
    }
    
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
    
    // ❌ 제거: getNextPendingStep() - 더 이상 필요 없음
    // ❌ 제거: areAllStepsApproved() - isLastStep()으로 대체
}
