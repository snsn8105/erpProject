package com.programpractice.approval_request_service.service;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.programpractice.approval_request_service.config.RabbitMQConfig;
import com.programpractice.approval_request_service.document.ApprovalRequest;
import com.programpractice.approval_request_service.dto.ApprovalRequestMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 승인 요청 메시지 발행 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalMessagePublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 승인 요청 메시지 발행
     */
    public void publishApprovalRequest(ApprovalRequestMessage message) {
        try {
            log.info("=== 승인 요청 메시지 발행 시작 ===");
            log.info("Exchange: {}", RabbitMQConfig.APPROVAL_EXCHANGE);
            log.info("RoutingKey: {}", RabbitMQConfig.APPROVAL_REQUEST_ROUTING_KEY);
            log.info("Message: approvalId={}, requesterId={}, title={}", 
                    message.getRequestId(), message.getRequesterId(), message.getTitle());
            
            // RabbitTemplate 상태 확인
            if (rabbitTemplate.getConnectionFactory() == null) {
                throw new IllegalStateException("RabbitMQ ConnectionFactory가 null입니다");
            }
            
            // 메시지 발행
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    RabbitMQConfig.APPROVAL_REQUEST_ROUTING_KEY,
                    message
            );
            
            log.info("=== 승인 요청 메시지 발행 완료 ===");
            
        } catch (Exception e) {
            log.error("=== 승인 요청 메시지 발행 실패 ===", e);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            
            // 스택 트레이스 출력
            e.printStackTrace();
            
            throw new RuntimeException("메시지 발행 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 다음 단계 처리를 위해 Processing Service로 메시지 발행
     * Listener에 있던 DTO 변환 로직을 이곳으로 캡슐화
     */
    public void publishNextStep(ApprovalRequest approvalRequest) {
        try {
            log.info("=== 다음 단계 메시지 발행 시작: requestId={} ===", approvalRequest.getRequestId());

            // 1. 다음 단계(CurrentStep) 정보 DTO 생성
            ApprovalRequestMessage.ApprovalStepDto nextStepDto = 
                ApprovalRequestMessage.ApprovalStepDto.builder()
                    .step(approvalRequest.getCurrentStepOrder())
                    .approverId(approvalRequest.getCurrentStep().getApproverId().longValue())
                    // 필요 시 status, comment 등은 초기화 상태로 보냄
                    .build();
            
            // 2. 전체 메시지 구성
            ApprovalRequestMessage message = ApprovalRequestMessage.builder()
                    .id(approvalRequest.getId())
                    .requestId(approvalRequest.getRequestId())
                    .requesterId(approvalRequest.getRequesterId().longValue())
                    .title(approvalRequest.getTitle())
                    .content(approvalRequest.getContent())
                    .steps(List.of(nextStepDto)) // 처리해야 할 다음 단계만 리스트에 담음
                    .requestedAt(approvalRequest.getCreatedAt())
                    .build();
            
            // 3. 발행 (기존 메서드 재사용)
            publishApprovalRequest(message);
            
            log.info(">> 다음 단계(Step {}) 발행 완료", approvalRequest.getCurrentStepOrder());

        } catch (Exception e) {
            log.error("다음 단계 메시지 발행 실패: requestId={}", approvalRequest.getRequestId(), e);
            throw new RuntimeException("메시지 발행 중 오류 발생", e);
        }
    }
}
