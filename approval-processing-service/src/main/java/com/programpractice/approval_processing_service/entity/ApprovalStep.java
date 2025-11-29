package com.programpractice.approval_processing_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "approval_steps")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApprovalStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    @Setter
    private ApprovalRequest approvalRequest;
    
    @Column(nullable = false)
    private Integer step;  // 단계 번호 (1, 2, 3, ...)
    
    @Column(nullable = false)
    private Long approverId;  // 승인자 ID
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String comment;  // 승인/반려 코멘트
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    /**
     * 승인 처리
     */
    public void approve(String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 반려 처리
     */
    public void reject(String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }
}