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
     * 이미 존재하는 요청은 무시 (덮어쓰기 방지)
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_REQUEST_QUEUE)
    public void handleApprovalRequest(ApprovalRequestMessage message) {
        log.info("=== RabbitMQ 메시지 수신 ===");
        log.info("requestId={}, requesterId={}", message.getRequestId(), message.getRequesterId());
        
        // 이미 존재하는지 확인
        if (processingService.existsByRequestId(message.getRequestId())) {
            log.info("⚠️ 이미 존재하는 요청 - 무시: requestId={}", message.getRequestId());
            log.info("(이 메시지는 다음 단계 진행을 위한 알림이므로 저장하지 않음)");
            return;
        }
        
        // 1. 신규 요청만 In-Memory DB에 저장
        log.info("✅ 신규 요청 - In-Memory DB에 저장 시작");
        processingService.processApprovalRequest(message);
        
        // 2. 사용자(Approver)의 REST API 호출을 대기
        log.info("요청 저장 완료. 사용자(Approver)의 REST API 호출을 대기합니다.");
    }
}
