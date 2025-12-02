package com.programpractice.approval_processing_service.service;

import java.time.LocalDate;
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
     * 1. 진행 중 (PENDING): 다음 단계 승인자에게 알림 (RequestMessage 발행)
     * 2. 종료 (APPROVED/REJECTED): 최종 결과 알림 (ResponseMessage 발행)
     */
    public void publishApprovalResult(ApprovalRequest approvalRequest) {
        // 메시지 객체를 try 블록 밖으로 빼서 예외 발생 시 로깅 가능하게 함 (단, 타입이 갈리므로 Object나 공통 인터페이스 사용 고려)
        Object payload;
        String routingKey;

        try {
            log.info("=== 승인 처리 결과 발행 시작 ===");
            log.info("Request ID: {}, Status: {}", approvalRequest.getRequestId(), approvalRequest.getFinalStatus());

            // 분기 로직: 전체 상태가 PENDING이면 아직 단계가 남은 것
            if (approvalRequest.getFinalStatus() == ApprovalStatus.PENDING) {
                // [Case 1] 다음 단계 진행 -> RequestMessage 발행
                routingKey = RabbitMQConfig.APPROVAL_REQUEST_ROUTING_KEY; // 주의: 다음 단계 승인자가 들을 키
                payload = createRequestMessage(approvalRequest);
                
                log.info(">> 다음 단계 진행을 위해 Request 메시지 발행");

            } else {
                // [Case 2] 최종 승인 또는 반려 -> ResponseMessage 발행
                // TODO: 반려 시 즉시 종료 로직 추가 필요
                routingKey = RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY; // 최종 결과를 들을 키
                payload = createResponseMessage(approvalRequest);
                
                log.info(">> 최종 결과({}) 통보를 위해 Response 메시지 발행", approvalRequest.getFinalStatus());
            }

            // 실제 발행
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    routingKey,
                    payload
            );
            
            log.info("=== 발행 완료 (RoutingKey: {}) ===", routingKey);
            
        } catch (Exception e) {
            log.error("=== 메시지 발행 실패 ===", e);
            log.error("Target RequestId: {}", approvalRequest.getRequestId());
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }

    // --- Private Helper Methods (메시지 생성 로직 분리) ---

    // 다음 단계 요청 메시지 생성
    private ApprovalRequestMessage createRequestMessage(ApprovalRequest request) {
        return ApprovalRequestMessage.builder()
                .id(request.getId())
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .content(request.getContent())
                
                // [핵심 변경] DTO의 모든 필드를 매핑합니다.
                .steps(request.getSteps().stream()
                    .map(step -> ApprovalRequestMessage.ApprovalStepDto.builder()
                        .step(step.getStep())
                        .approverId(step.getApproverId())
                        
                        // 1. Enum(Entity) -> String(DTO) 변환
                        // status가 null이 아님을 보장하거나 null safe하게 처리해야 함
                        .status(step.getStatus() != null ? step.getStatus().name() : null)
                        
                        // 2. 코멘트 전달 (null일 수 있음)
                        .comment(step.getComment())
                        
                        // 3. 처리 시간 전달 (null일 수 있음)
                        .processedAt(step.getProcessedAt())
                        
                        .build())
                    .toList())
                
                .requestedAt(request.getCreatedAt())
                .build();
    }

    // 최종 결과 메시지 생성
    private ApprovalResponseMessage createResponseMessage(ApprovalRequest request) {
        return ApprovalResponseMessage.builder()
                .requestId(request.getRequestId())
                .finalStatus(request.getFinalStatus().name()) // APPROVED or REJECTED
                .updatedAt(LocalDateTime.now())
                .processedAt(request.getUpdatedAt()) // 최종 업데이트 시간
                .build();
    }
}