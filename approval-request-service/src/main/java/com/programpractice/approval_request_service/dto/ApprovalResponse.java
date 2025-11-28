package com.programpractice.approval_request_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.programpractice.approval_request_service.document.ApprovalRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ApprovalResponse {
    
    private Integer requestId;
    private Integer requesterId;
    private String title;
    private String content;
    private List<StepResponse> steps;
    private String finalStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ApprovalResponse from(ApprovalRequest request) {
        return ApprovalResponse.builder()
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .content(request.getContent())
                .steps(request.getSteps().stream()
                        .map(StepResponse::from)
                        .collect(Collectors.toList()))
                .finalStatus(request.getFinalStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
