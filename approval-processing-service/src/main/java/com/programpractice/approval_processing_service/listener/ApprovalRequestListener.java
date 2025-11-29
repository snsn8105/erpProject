package com.programpractice.approval_processing_service.listener;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.programpractice.approval_processing_service.config.RabbitMQConfig;
import com.programpractice.approval_processing_service.dto.ApprovalRequestMessage;
import com.programpractice.approval_processing_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_processing_service.service.ApprovalProcessingService;
import com.programpractice.approval_processing_service.service.ApprovalResponsePublisher;

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
    private final ApprovalResponsePublisher responsePublisher;
    
    /**
     * 승인 요청 메시지 수신 및 처리
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_REQUEST_QUEUE)
    public void handleApprovalRequest(ApprovalRequestMessage message) {
        log.info("=== 승인 요청 메시지 수신 ===");
        log.info("approvalId: {}", message.getRequestId());
        log.info("requesterId: {}", message.getRequesterId());
        log.info("title: {}", message.getTitle());
        
        ApprovalResponseMessage response;
        
        try {
            // 승인 요청 처리 (H2 In-Memory DB에 저장)
            response = processingService.processApprovalRequest(message);
            
            log.info("승인 요청 처리 완료: approvalId={}, status={}", 
                    message.getRequestId(), response.getStatus());
            
        } catch (Exception e) {
            log.error("승인 요청 처리 중 오류 발생: approvalId={}", 
                    message.getRequestId(), e);
            
            // 에러 응답 생성
            response = ApprovalResponseMessage.builder()
                    .approvalId(message.getRequestId())
                    .status("ERROR")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .processedAt(LocalDateTime.now())
                    .build();
        }
        
        // 응답 메시지 발행
        responsePublisher.publishApprovalResult(response);
    }
}