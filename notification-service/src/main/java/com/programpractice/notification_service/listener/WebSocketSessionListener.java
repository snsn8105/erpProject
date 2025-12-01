package com.programpractice.notification_service.listener;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.programpractice.notification_service.service.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// WebSocket 연결/종료 이벤트 리스너
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionListener {
    
    private final WebSocketSessionManager sessionManager;

    // WebSocket 연결 이벤트
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();   

        String employeeId = extractEmployeeId(headerAccessor);

        if (employeeId != null && sessionId != null) {
            sessionManager.addSession(employeeId, sessionId);
            log.info("WebSocket 연결됨: employeeId={}, sessionId={}", employeeId, sessionId);
        } else {
            log.warn("employeeId 또는 sessionId가 없음: employeeId={}, sessionId={}", 
                    employeeId, sessionId);
        }        
    }

    // WebSocket 구독 이벤트
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        log.info("WebSocket 구독: sessionId={}, destination={}", sessionId, destination);
    }
    
    // WebSocket 연결 해제 이벤트
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        if (sessionId != null) {
            String employeeId = sessionManager.getEmployeeId(sessionId);
            sessionManager.removeSession(sessionId);
            log.info("WebSocket 연결 해제됨: employeeId={}, sessionId={}", employeeId, sessionId);
        }
    }
    
    // STOMP 헤더에서 employeeId 추출
    private String extractEmployeeId(StompHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) return null;

        // 2. 세션 속성에서 꺼냄
        return (String) sessionAttributes.get("employeeId");
    }   
}
