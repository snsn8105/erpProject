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
            log.info("=== 승인 처리 결과 발행 ===");
            log.info("approvalId: {}", message.getApprovalId());
            log.info("step: {}", message.getStep());
            log.info("status: {}", message.getStatus());
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY,
                    message
            );
            
            log.info("승인 처리 결과 발행 완료: approvalId={}", message.getApprovalId());
            
        } catch (Exception e) {
            log.error("승인 처리 결과 발행 실패: approvalId={}", 
                    message.getApprovalId(), e);
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }
}
