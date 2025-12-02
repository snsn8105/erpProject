// approval-processing-service/src/main/java/com/programpractice/approval_processing_service/model/ApprovalRequest.java
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
    private Integer currentStepOrder = 1;  // 현재 단계 번호
    
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

    // 현재 단계 가져오기 (O(1) 접근)
    public ApprovalStep getCurrentStep() {
        if (this.finalStatus != ApprovalStatus.PENDING) {
            return null;
        }
        
        int index = this.currentStepOrder - 1;
        
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
    
}
