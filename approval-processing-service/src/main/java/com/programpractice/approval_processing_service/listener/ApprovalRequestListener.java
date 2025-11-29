package com.programpractice.approval_processing_service.listener;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.programpractice.approval_processing_service.config.RabbitMQConfig;
import com.programpractice.approval_processing_service.dto.ApprovalRequestMessage;
import com.programpractice.approval_processing_service.dto.ApprovalResponseMessage;
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
    
    private final RabbitTemplate rabbitTemplate;
    private final ApprovalProcessingService processingService;
    
    /**
     * 승인 요청 메시지 수신 및 처리
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_REQUEST_QUEUE)
    public void handleApprovalRequest(ApprovalRequestMessage message) {
        log.info("승인 요청 메시지 수신: approvalId={}, requesterId={}", 
                message.getRequestId(), message.getRequesterId());
        
        ApprovalResponseMessage response;
        
        try {
            // 승인 요청 처리
            response = processingService.processApprovalRequest(message);
            
            log.info("승인 요청 처리 완료: approvalId={}, status={}", 
                    message.getRequestId(), response.getStatus());
            
        } catch (Exception e) {
            log.error("승인 요청 처리 중 오류 발생: approvalId={}", 
                    message.getRequestId(), e);
            
            // 에러 응답 생성
            response = ApprovalResponseMessage.builder()
                    .requestId(message.getRequestId())
                    .status("ERROR")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        
        // 응답 메시지 발행
        publishResponse(response);
    }
    
    /**
     * 승인 응답 메시지 발행
     */
    private void publishResponse(ApprovalResponseMessage response) {
        try {
            log.info("승인 응답 메시지 발행: requestId={}", response.getRequestId());
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY,
                    response
            );
            
            log.info("승인 응답 메시지 발행 완료: requestId={}", response.getRequestId());
            
        } catch (Exception e) {
            log.error("승인 응답 메시지 발행 실패: requestId={}", 
                    response.getRequestId(), e);
        }
    }
}