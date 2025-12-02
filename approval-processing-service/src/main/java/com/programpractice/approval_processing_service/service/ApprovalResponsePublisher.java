// approval-processing-service/src/main/java/com/programpractice/approval_processing_service/service/ApprovalResponsePublisher.java
package com.programpractice.approval_processing_service.service;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.programpractice.approval_processing_service.config.RabbitMQConfig;
import com.programpractice.approval_processing_service.dto.ApprovalRequestMessage;
import com.programpractice.approval_processing_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_processing_service.model.ApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponsePublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 승인 처리 결과 발행
     * 
     * 분기 로직:
     * 1. finalStatus가 PENDING (진행 중): 다음 단계 승인자에게 RequestMessage 발행
     * 2. finalStatus가 APPROVED/REJECTED (종료): 최종 결과 ResponseMessage 발행
     */
    public void publishApprovalResult(ApprovalRequest approvalRequest) {
        try {
            log.info("=== 메시지 발행 시작 ===");
            log.info("requestId={}, finalStatus={}, currentStep={}/{}", 
                    approvalRequest.getRequestId(),
                    approvalRequest.getFinalStatus(),
                    approvalRequest.getCurrentStepOrder(),
                    approvalRequest.getSteps().size());

            // 분기: PENDING이면 다음 단계 진행, 아니면 최종 결과 통보
            if (approvalRequest.getFinalStatus() == ApprovalStatus.PENDING) {
                
                // [Case 1] 다음 단계 진행
                publishNextStepRequest(approvalRequest);
                
            } else {
                
                // [Case 2] 최종 승인/반려 결과 통보
                publishFinalResult(approvalRequest);
            }
            
        } catch (Exception e) {
            log.error("=== 메시지 발행 실패 ===", e);
            log.error("requestId={}", approvalRequest.getRequestId());
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }

    /**
     * 다음 단계 요청 메시지 발행
     */
    private void publishNextStepRequest(ApprovalRequest request) {
        log.info("➡️ 다음 단계 진행을 위한 RequestMessage 발행");
        
        ApprovalRequestMessage message = createRequestMessage(request);
        
        log.info("발행 대상: Exchange={}, RoutingKey={}", 
                RabbitMQConfig.APPROVAL_EXCHANGE,
                RabbitMQConfig.APPROVAL_REQUEST_ROUTING_KEY);
        
        if (request.getCurrentStep() != null) {
            log.info("다음 승인 대기자: approverId={}, step={}", 
                    request.getCurrentStep().getApproverId(),
                    request.getCurrentStep().getStep());
        }
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.APPROVAL_EXCHANGE,
                RabbitMQConfig.APPROVAL_REQUEST_ROUTING_KEY,
                message
        );
        
        log.info("✅ RequestMessage 발행 완료");
    }

    /**
     * 최종 결과 메시지 발행
     */
    private void publishFinalResult(ApprovalRequest request) {
        log.info("🏁 최종 결과({}) 통보를 위한 ResponseMessage 발행", 
                request.getFinalStatus());
        
        ApprovalResponseMessage message = createResponseMessage(request);
        
        log.info("발행 대상: Exchange={}, RoutingKey={}", 
                RabbitMQConfig.APPROVAL_EXCHANGE,
                RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY);
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.APPROVAL_EXCHANGE,
                RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY,
                message
        );
        
        log.info("✅ ResponseMessage 발행 완료: finalStatus={}", message.getFinalStatus());
    }

    // --- Private Helper Methods ---

    /**
     * 다음 단계 요청 메시지 생성
     */
    private ApprovalRequestMessage createRequestMessage(ApprovalRequest request) {
        return ApprovalRequestMessage.builder()
                .id(request.getId())
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .content(request.getContent())
                .steps(request.getSteps().stream()
                    .map(step -> ApprovalRequestMessage.ApprovalStepDto.builder()
                        .step(step.getStep())
                        .approverId(step.getApproverId())
                        .status(step.getStatus() != null ? step.getStatus().name() : null)
                        .comment(step.getComment())
                        .processedAt(step.getProcessedAt())
                        .build())
                    .toList())
                .requestedAt(request.getCreatedAt())
                .build();
    }

    /**
     * 최종 결과 메시지 생성
     */
    private ApprovalResponseMessage createResponseMessage(ApprovalRequest request) {
        return ApprovalResponseMessage.builder()
                .id(request.getId())
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId().intValue())
                .title(request.getTitle())
                .finalStatus(request.getFinalStatus().name())
                .updatedAt(LocalDateTime.now())
                .processedAt(request.getUpdatedAt())
                .success(true)
                .build();
    }
}
