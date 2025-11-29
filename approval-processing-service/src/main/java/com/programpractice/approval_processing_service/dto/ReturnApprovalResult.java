package com.programpractice.approval_processing_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 내부 로직 반환용 결과 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnApprovalResult implements Serializable {
    private Integer step;                // 단계 번호
    private Long approverId;             // 승인자 ID
    private String status;               // 단계 상태
    private LocalDateTime updatedAt;     // 업데이트 시간
}
