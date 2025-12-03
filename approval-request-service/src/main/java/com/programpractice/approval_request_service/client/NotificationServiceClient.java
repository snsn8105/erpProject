package com.programpractice.approval_request_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.programpractice.approval_request_service.dto.NotificationRequest;

/**
 * Notification Service REST API 클라이언트
 * 최종 승인/반려 결과를 Notification Service에 전달
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:http://localhost:8084}")
    private String notificationServiceUrl;

    // 승인 결과 알림 전송
    public boolean sendApprovalNotification(
            Integer requestId,
            Integer requesterId, 
            String title,
            String finalStatus,
            Integer rejectedBy) {
        
        String url = notificationServiceUrl + "/api/notifications/send";
        
        try {
            log.info("Notification Service 호출 시작: url={}", url);
            log.info("requestId={}, requesterId={}, finalStatus={}", 
                    requestId, requesterId, finalStatus);
            
            // 요청 DTO 생성
            NotificationRequest request = NotificationRequest.builder()
                    .requestId(requestId)
                    .requesterId(requesterId)
                    .title(title)
                    .finalStatus(finalStatus)
                    .rejectedBy(rejectedBy)
                    .build();
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, headers);
            
            // POST 요청
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, 
                    entity, 
                    String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ Notification Service 호출 성공: requestId={}", requestId);
                return true;
            } else {
                log.warn("⚠️ Notification Service 응답 상태 코드: {}", 
                        response.getStatusCode());
                return false;
            }
            
        } catch (HttpClientErrorException e) {
            log.error("❌ Notification Service 호출 실패 (클라이언트 에러): " +
                    "status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return false;
            
        } catch (Exception e) {
            log.error("❌ Notification Service 호출 실패 (예외 발생): " +
                    "requestId={}, error={}", 
                    requestId, e.getMessage(), e);
            return false;
        }
    }
}
