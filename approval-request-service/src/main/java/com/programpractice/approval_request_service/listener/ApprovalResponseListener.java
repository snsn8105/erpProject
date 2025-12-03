package com.programpractice.approval_request_service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.programpractice.approval_request_service.config.RabbitMQConfig;
import com.programpractice.approval_request_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_request_service.service.ResponseHandleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponseListener {
    
    private final ResponseHandleService responseHandleService;
    
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_RESPONSE_QUEUE)
    public void handleApprovalResponse(ApprovalResponseMessage message) {
        log.info("=== 승인 응답 수신: requestId={} ===", message.getRequestId());
        
        try {
            // 모든 비즈니스 로직을 서비스로 위임
            responseHandleService.handleResponse(message);
            
        } catch (Exception e) {
            log.error("메시지 처리 중 치명적 오류", e);
            // 필요하다면 Dead Letter Queue(DLQ)로 보내거나 재시도 로직 추가
        }
    }
}
