package com.programpractice.notification_service.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.programpractice.notification_service.dto.NotificationMessage;
import com.programpractice.notification_service.service.NotificationService;
import com.programpractice.notification_service.service.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 * 연결 상태 확인 및 알림 전송
 */
@RestController
@RequiredArgsConstructor
@Slf4j
class NotificationRestController {
    
    private final WebSocketSessionManager sessionManager;
    private final NotificationService notificationService;
    
    /**
     * 승인 결과 알림 전송 API
     * Approval Request Service에서 호출
     * 
     * POST /api/notifications/send
     * {
     *   "requestId": 1,
     *   "requesterId": 101,
     *   "title": "휴가 신청",
     *   "finalStatus": "approved",
     *   "rejectedBy": null
     * }
     */
    @PostMapping("/api/notifications/send")
    public ResponseEntity<Map<String, Object>> sendApprovalNotification(
            @RequestBody NotificationRequest request) {
        
        log.info("=== 승인 알림 전송 API 호출 ===");
        log.info("requestId={}, requesterId={}, finalStatus={}, rejectedBy={}", 
                request.getRequestId(), 
                request.getRequesterId(), 
                request.getFinalStatus(),
                request.getRejectedBy());
        
        try {
            // NotificationMessage 생성
            String message = createNotificationMessage(
                    request.getFinalStatus(), 
                    request.getRejectedBy());
            
            NotificationMessage notification = NotificationMessage.builder()
                    .requestId(request.getRequestId())
                    .result(request.getFinalStatus())
                    .finalResult(request.getFinalStatus())
                    .rejectedBy(request.getRejectedBy())
                    .title(request.getTitle())
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // 요청자에게 알림 전송
            notificationService.sendToEmployee(
                    request.getRequesterId().toString(), 
                    notification);
            
            log.info("✅ 승인 알림 전송 완료");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "알림이 전송되었습니다");
            response.put("requestId", request.getRequestId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 승인 알림 전송 실패", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "알림 전송 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 알림 메시지 생성
     */
    private String createNotificationMessage(String finalStatus, Integer rejectedBy) {
        if ("approved".equalsIgnoreCase(finalStatus)) {
            return "🎉 승인 요청이 최종 승인되었습니다.";
        } else if ("rejected".equalsIgnoreCase(finalStatus)) {
            if (rejectedBy != null) {
                return "❌ 승인 요청이 반려되었습니다. (반려자 ID: " + rejectedBy + ")";
            } else {
                return "❌ 승인 요청이 반려되었습니다.";
            }
        } else {
            return "ℹ️ 승인 요청 상태가 업데이트되었습니다.";
        }
    }
    
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

/**
 * Notification 요청 DTO
 * Approval Request Service에서 받는 요청
 */
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class NotificationRequest {
    private Integer requestId;      // 승인 요청 ID
    private Integer requesterId;    // 요청자 ID (알림 받을 사람)
    private String title;           // 승인 요청 제목
    private String finalStatus;     // 최종 상태 (approved, rejected)
    private Integer rejectedBy;     // 반려한 승인자 ID (반려 시에만)
}
