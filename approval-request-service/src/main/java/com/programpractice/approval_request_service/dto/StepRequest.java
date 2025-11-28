package com.programpractice.approval_request_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

// 결재 단계 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepRequest {
    
    @NotNull(message = "단계 번호는 필수입니다")
    @Positive(message = "단계 번호는 양수여야 합니다")
    private Integer step;
    
    @NotNull(message = "결재자 ID는 필수입니다")
    @Positive(message = "결재자 ID는 양수여야 합니다")
    private Integer approverId;
}
