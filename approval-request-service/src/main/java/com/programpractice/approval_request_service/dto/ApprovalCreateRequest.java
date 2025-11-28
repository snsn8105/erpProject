package com.programpractice.approval_request_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

// 결제 요청 생성 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalCreateRequest {
    
    @NotNull(message = "요청자 ID는 필수입니다")
    @Positive(message = "요청자 ID는 양수여야 합니다")
    private Integer requesterId;
    
    @NotBlank(message = "제목은 필수입니다")
    private String title;
    
    @NotBlank(message = "내용은 필수입니다")
    private String content;
    
    @NotEmpty(message = "결재 단계는 최소 1개 이상이어야 합니다")
    private List<StepRequest> steps;
}
