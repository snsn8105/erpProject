package com.programpractice.approval_request_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.programpractice.approval_request_service.config.RabbitMQConfig;
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
}
