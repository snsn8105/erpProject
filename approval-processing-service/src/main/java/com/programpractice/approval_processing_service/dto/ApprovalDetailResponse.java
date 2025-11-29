package com.programpractice.approval_processing_service.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API: 승인 요청 상세 응답 (Response Body)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDetailResponse {
    private Long id;                     // RDB(MySQL)의 ID (혹은 MongoDB ID와 매핑 필요 시 String 변경 고려)
    private String requestId;            // MongoDB ID
    private Long requesterId;
    private String title;
    private String content;
    private String finalStatus;
    private List<ApprovalStepDto> steps; // 하위 단계 리스트
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
