package com.programpractice.approval_processing_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 승인 요청 메시지 (RabbitMQ Consumer용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestMessage implements Serializable {
    private String id;                 // MongoDB의 _id (String)
    private Integer requestId;            // 요청 ID
    private Long requesterId;            // 요청자 ID
    private String requesterName;        // 요청자 이름
    private String title;                // 제목
    private String content;              // 내용

    private List<ApprovalStepDto> steps; // 승인 단계 정보

    private LocalDateTime requestedAt;   // 요청 시간

    // 내부 클래스로 정의
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder // Builder 추가 추천
    public static class ApprovalStepDto implements Serializable {
        private Integer step;
        private Long approverId;
    }
}
