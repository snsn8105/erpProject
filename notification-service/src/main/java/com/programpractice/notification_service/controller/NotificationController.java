package com.programpractice.notification_service.controller;

import com.programpractice.notification_service.dto.NotificationMessage;
import com.programpractice.notification_service.service.NotificationService;
import com.programpractice.notification_service.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 알림 컨트롤러
 * REST API + WebSocket 메시지 핸들링
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 클라이언트가 메시지를 보낼 때 처리
     * (예: ping-pong, 연결 테스트)
     */
    @MessageMapping("/ping/{employeeId}")
    @SendTo("/topic/notifications/{employeeId}")
    public NotificationMessage handlePing(@DestinationVariable String employeeId) {
        log.info("Ping 메시지 수신: employeeId={}", employeeId);
        
        return NotificationMessage.builder()
                .message("pong")
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }
}

/**
 * REST API 컨트롤러
 * 연결 상태 확인 등
 */
@RestController
@RequiredArgsConstructor
@Slf4j
class NotificationRestController {
    
    private final WebSocketSessionManager sessionManager;
    private final NotificationService notificationService;
    
    /**
     * 특정 직원의 연결 상태 확인
     */
    @GetMapping("/api/notifications/status/{employeeId}")
    public ResponseEntity<Map<String, Object>> checkConnectionStatus(@PathVariable String employeeId) {
        
        boolean connected = sessionManager.isConnected(employeeId);
        int sessionCount = sessionManager.getSessionIds(employeeId).size();
        
        Map<String, Object> response = new HashMap<>();
        response.put("employeeId", employeeId);
        response.put("connected", connected);
        response.put("sessionCount", sessionCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 전체 연결 통계
     */
    @GetMapping("/api/notifications/stats")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("connectedUsers", sessionManager.getConnectedUserCount());
        stats.put("totalSessions", sessionManager.getTotalSessionCount());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 테스트용: 특정 직원에게 알림 전송
     */
    @GetMapping("/api/notifications/test/{employeeId}")
    public ResponseEntity<String> sendTestNotification(@PathVariable String employeeId) {
        
        NotificationMessage testMessage = NotificationMessage.builder()
                .requestId(999)
                .result("approved")
                .finalResult("approved")
                .title("테스트 알림")
                .message("이것은 테스트 알림입니다.")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        notificationService.sendToEmployee(employeeId, testMessage);
        
        return ResponseEntity.ok("테스트 알림 전송 완료: employeeId=" + employeeId);
    }
}
