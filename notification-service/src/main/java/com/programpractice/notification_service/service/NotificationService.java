package com.programpractice.notification_service.service;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.programpractice.notification_service.dto.ApprovalResponseMessage;
import com.programpractice.notification_service.dto.NotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 알림 전송 서비스
// WebSocket을 통해 클라이언트에게 실시간 알림 전송
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    // 승인 결과 알림 전송
    public void sendApprovalNotification(ApprovalResponseMessage message) {
        // 요청자에게 알림 전송
        if (message.getRequestId() != null) {
            sendToEmployee(message.getRequesterId().toString(), createNotificationMessage(message));
        }

        log.info("승인 알림 전송 완료: requestId={}, requesterId={}, status={}", 
                message.getRequestId(), message.getRequesterId(), message.getFinalStatus());
    }

    // 특정 직원에게 알림 전송
    public void sendToEmployee(String employeeId, NotificationMessage notificationMessage) {
        // 해당 직원이 연결되어 있는지 확인
        if (!sessionManager.isConnected(employeeId)) {
            log.warn("직원이 연결되어 있지 않음: employeeId={}", employeeId);
            return;
        }

        // /topic/notifications/{employeeID}로 메시지 발행
        String destination = "/topic/notifications" + employeeId;
        messagingTemplate.convertAndSend(destination, notificationMessage);

        log.info("WebSocket 알림 전송: destination={}, employeeId={}, message={}", 
                destination, employeeId, notificationMessage.getMessage());
    }

    // 전체 직원에게 브로드캐스트
    public void broadcastToAll(NotificationMessage notification) {
        messagingTemplate.convertAndSend("/topic/notifications/all", notification);
        log.info("전체 브로드캐스트 전송: message={}", notification.getMessage());
    }
    
    // NotificationMessage 생성
    private NotificationMessage createNotificationMessage(ApprovalResponseMessage message) {
        
        String result = message.getStatus(); // APPROVED, REJECTED, PENDING
        String finalResult = message.getFinalStatus();
        Integer rejectedBy = null;
        String notificationMessage = "";

        // 메시지 생성
        if ("approved".equalsIgnoreCase(finalResult)) {
            notificationMessage = "승인 요청이 최종 승인 되었습니다.";
        } else if ("rejected".equalsIgnoreCase(result)) {
            notificationMessage = "승인 요청이 반려 되었습니다. (반려자 ID: " + rejectedBy + ")";
        } else if ("approved".equalsIgnoreCase(result)) {
            notificationMessage = message.getStep() + " 단계가 통과되었습니다. (승인 진행 중)";
        }

        return NotificationMessage.builder()
                .requestId(message.getNumericRequestId())
                .result(result)
                .finalResult(finalResult)
                .rejectedBy(rejectedBy)
                .title(message.getTitle())
                .message(notificationMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
