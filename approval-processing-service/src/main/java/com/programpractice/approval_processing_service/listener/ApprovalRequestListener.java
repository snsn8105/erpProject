package com.programpractice.approval_processing_service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.programpractice.approval_processing_service.config.RabbitMQConfig;
import com.programpractice.approval_processing_service.dto.ApprovalRequestMessage;
import com.programpractice.approval_processing_service.service.ApprovalProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 승인 요청 메시지 수신 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalRequestListener {
    
    private final ApprovalProcessingService processingService;
    
    /**
     * 승인 요청 메시지 수신 및 처리
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_REQUEST_QUEUE)
    public void handleApprovalRequest(ApprovalRequestMessage message) {
        // 1. In-Memory DB에 저장
        processingService.processApprovalRequest(message);
        
        // 2. 사용자(Approver)의 REST API 호출을 대기
        log.info("요청 저장 완료. 사용자(Approver)의 REST API 호출을 대기합니다.");
    }
}
