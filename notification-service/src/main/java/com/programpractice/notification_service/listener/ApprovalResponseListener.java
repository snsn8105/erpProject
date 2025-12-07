package com.programpractice.notification_service.listener;

import com.programpractice.notification_service.dto.ApprovalResponseMessage;
import com.programpractice.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// RabbitMQ 승인 응답 메시지 리스너
// Approval Request Service로부터 메시지를 받아 WebSocket으로 전달
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponseListener {
    
    private final NotificationService notificationService;
    
    /**
     * 승인 응답 메시지 수신 및 WebSocket 전송
     */
    // @RabbitListener(queues = RabbitMQConfig.APPROVAL_RESPONSE_QUEUE)
    public void handleApprovalResponse(ApprovalResponseMessage message) {
        try {
            log.info("=== 승인 응답 메시지 수신 ===");
            log.info("requestId: {}", message.getRequestId());
            log.info("status: {}", message.getStatus());
            log.info("finalStatus: {}", message.getFinalStatus());
            
            if (!message.isSuccess()) {
                log.error("승인 처리 실패 메시지: {}", message.getErrorMessage());
                return;
            }
            
            // WebSocket으로 알림 전송
            notificationService.sendApprovalNotification(message);
            
            log.info("WebSocket 알림 전송 완료");
            
        } catch (Exception e) {
            log.error("승인 응답 메시지 처리 중 오류 발생", e);
        }
    }
}
