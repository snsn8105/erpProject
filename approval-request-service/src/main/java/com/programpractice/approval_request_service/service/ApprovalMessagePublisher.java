package com.programpractice.approval_request_service.service;

import com.programpractice.approval_request_service.config.RabbitMQConfig;
import com.programpractice.approval_request_service.dto.ApprovalRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

// 승인 요청 메시지 발행 서비스

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalMessagePublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    // 승인 요청 메시지 발행
    public void publishApprovalRequest(ApprovalRequestMessage message) {
        try {
            log.info("승인 요청 메시지 발행 시작: approvalId={}", message.getApprovalId());
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    RabbitMQConfig.APPROVAL_REQUEST_ROUTING_KEY,
                    message
            );
            
            log.info("승인 요청 메시지 발행 완료: approvalId={}", message.getApprovalId());
            
        } catch (Exception e) {
            log.error("승인 요청 메시지 발행 실패: approvalId={}", message.getApprovalId(), e);
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }
}
