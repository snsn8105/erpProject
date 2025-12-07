package com.programpractice.approval_processing_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Employee Service REST API 클라이언트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${employee.service.url:http://localhost:8081}")
    private String employeeServiceUrl;
    
    /**
     * 직원 존재 여부 확인
     * 
     * @param employeeId 직원 ID
     * @return 존재 여부
     */
    public boolean existsEmployee(Long employeeId) {
        String url = employeeServiceUrl + "/employees/" + employeeId;
        
        try {
            log.debug("Employee Service 호출: url={}, employeeId={}", url, employeeId);
            
            // GET 요청으로 직원 정보 조회
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // 200 OK이면 직원 존재
            boolean exists = response.getStatusCode() == HttpStatus.OK;
            
            log.debug("직원 존재 확인 완료: employeeId={}, exists={}", employeeId, exists);
            return exists;
            
        } catch (HttpClientErrorException.NotFound e) {
            // 404 Not Found → 직원이 존재하지 않음 (정상적인 케이스)
            log.debug("직원이 존재하지 않음 (404): employeeId={}", employeeId);
            return false;
            
        } catch (HttpClientErrorException e) {
            // 기타 4xx 에러 (400, 401, 403 등)
            log.error("Employee Service 호출 실패 (클라이언트 에러): " +
                    "employeeId={}, status={}, message={}", 
                    employeeId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("직원 정보 확인 실패: " + e.getMessage(), e);
            
        } catch (Exception e) {
            // 5xx 서버 에러 또는 네트워크 에러
            log.error("Employee Service 호출 실패 (서버/네트워크 에러): employeeId={}", 
                    employeeId, e);
            throw new RuntimeException("Employee Service 연결 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 직원 존재 여부 확인 (exists API 사용)
     * Employee Service에 /employees/{id}/exists API가 있는 경우
     * 
     * @param employeeId 직원 ID
     * @return 존재 여부
     */
    public boolean existsEmployeeByExistsApi(Long employeeId) {
        String url = employeeServiceUrl + "/employees/" + employeeId + "/exists";
        
        try {
            log.debug("Employee Service 호출 (exists API): url={}", url);
            
            Boolean exists = restTemplate.getForObject(url, Boolean.class);
            
            log.debug("직원 존재 확인 완료: employeeId={}, exists={}", employeeId, exists);
            return Boolean.TRUE.equals(exists);
            
        } catch (Exception e) {
            log.error("Employee Service 호출 실패: employeeId={}", employeeId, e);
            throw new RuntimeException("Employee Service 연결 실패: " + e.getMessage(), e);
        }
    }
}