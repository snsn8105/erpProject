package com.programpractice.approval_processing_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.programpractice.approval_processing_service.config.RabbitMQConfig;
import com.programpractice.approval_processing_service.dto.ApprovalResponseMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponsePublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 승인 처리 결과 발행 (REST API 처리 후 호출)
     */
    public void publishApprovalResult(ApprovalResponseMessage message) {
        try {
            log.info("=== 승인 처리 결과 발행 시작 ===");
            log.info("requestId: {}", message.getRequestId());
            log.info("step: {}", message.getStep());
            log.info("approverId: {}", message.getApproverId());
            log.info("status: {}", message.getStatus());
            log.info("Exchange: {}", RabbitMQConfig.APPROVAL_EXCHANGE);
            log.info("RoutingKey: {}", RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY);
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY,
                    message
            );
            
            log.info("=== 승인 처리 결과 발행 완료 ===");
            
        } catch (Exception e) {
            log.error("=== 승인 처리 결과 발행 실패 ===", e);
            log.error("requestId: {}", message.getRequestId());
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }
}
