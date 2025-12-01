package com.programpractice.notification_service.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// WebSocket 세션 관리
// employeeId와 sessionId 매핑을 In-Memory Map으로 관리
@Component
@Slf4j
public class WebSocketSessionManager {

    // employeeId -> Set<sessionId> 매핑
    private final Map<String, Set<String>> employeeSessionMap = new ConcurrentHashMap<>();

    // sessionId -> employeeId 역방향 매핑
    private final Map<String, String> sessionEmployeeMap = new ConcurrentHashMap<>();

    // 세션 등록
    public void addSession(String employeeId, String sessionId) {
        employeeSessionMap.computeIfAbsent(employeeId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        
        sessionEmployeeMap.put(sessionId, employeeId);

        log.info("WebSocket 세션 등록: employeeId={}, sessionId={}", employeeId, sessionId);
        log.info("현재 연결된 사용자 수: {}", employeeSessionMap.size());
    }

    // 세선 제거
    public void removeSession(String sessionId) {
        String employeeId = sessionEmployeeMap.remove(sessionId);
        if (employeeId != null) {
            Set<String> sessions = employeeSessionMap.get(employeeId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    employeeSessionMap.remove(employeeId);
                }
            }
            log.info("WebSocket 세션 제거: employeeId={}, sessionId={}", employeeId, sessionId);
        }
    }
    
    // 특정 직원의 모든 세션 ID 조회
    public Set<String> getSessionIds(String employeeId) {
        return employeeSessionMap.getOrDefault(employeeId, Set.of());
    }
    
    // 세션 ID로 직원 ID 조회
    public String getEmployeeId(String sessionId) {
        return sessionEmployeeMap.get(sessionId);
    }
    
    // 특정 직원이 연결되어 있는지 확인
    public boolean isConnected(String employeeId) {
        Set<String> sessions = employeeSessionMap.get(employeeId);
        return sessions != null && !sessions.isEmpty();
    }
    
    // 전체 연결된 직원 수
    public int getConnectedUserCount() {
        return employeeSessionMap.size();
    }
    
    // 전체 세션 수
    public int getTotalSessionCount() {
        return sessionEmployeeMap.size();
    }
}
