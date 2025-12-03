package com.programpractice.approval_request_service.document;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 결재 단계 내장 문서
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Step {
    
    private Integer step;
    private Integer approverId;
    private String status;  // pending, approved, rejected
    private LocalDateTime updatedAt;
    
    public void updateStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
