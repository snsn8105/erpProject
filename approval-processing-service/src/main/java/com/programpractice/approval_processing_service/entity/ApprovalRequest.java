package com.programpractice.approval_processing_service.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 승인 요청 엔티티 (In-Memory 저장)
 */
@Entity
@Table(name = "approval_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ApprovalRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String requestId;  // MongoDB의 approvalId와 매핑
    
    @Column(nullable = false)
    private Long requesterId;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApprovalStep> steps = new ArrayList<>();
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus finalStatus = ApprovalStatus.PENDING;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 승인 단계 추가
     */
    public void addStep(ApprovalStep step) {
        steps.add(step);
        step.setApprovalRequest(this);
    }
    
    /**
     * 최종 상태 업데이트
     */
    public void updateFinalStatus(ApprovalStatus status) {
        this.finalStatus = status;
    }
    
    /**
     * 다음 PENDING 단계 찾기
     */
    public ApprovalStep getNextPendingStep() {
        return steps.stream()
                .filter(step -> step.getStatus() == ApprovalStatus.PENDING)
                .findFirst()
                .orElse(null);
    }
}

