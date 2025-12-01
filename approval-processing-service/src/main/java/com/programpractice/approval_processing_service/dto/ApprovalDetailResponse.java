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
    private String id;                     // MongoDB의 _id (String)
    private Integer requestId;            // 요청 ID
    private Long requesterId;
    private String title;
    private String content;
    private String finalStatus;
    private List<ApprovalStepDto> steps; // 하위 단계 리스트
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
