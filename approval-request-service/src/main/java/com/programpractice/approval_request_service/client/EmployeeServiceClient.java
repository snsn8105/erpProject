package com.programpractice.approval_request_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

// Employee Service REST API 클라이언트
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceClient {

    private final RestTemplate restTemplate;

    @Value("${employee.service.url}")
    private String employeeServiceUrl;

    // 직원 존재 여부 확인
    public boolean existsEmployee(Integer employeeId) {
        // API 명세에 맞춰 URL 설정
        String url = employeeServiceUrl + "/employees/" + employeeId;

        try {
            log.info("Employee Service 호출: {}", url);

            // getForEntity를 사용하여 상태 코드와 본문을 함께 확인
            // 200 OK가 떨어지면 직원이 존재하는 것으로 간주
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;

        } catch (HttpClientErrorException.NotFound e) {
            // 404 Not Found 에러는 "직원이 없음"을 의미하므로 false 반환 (에러 아님)
            log.info("직원이 존재하지 않음 (404 Not Found): employeeId={}", employeeId);
            return false;

        } catch (Exception e) {
            // 그 외의 에러(서버 다운, 500 에러 등)는 진짜 예외로 던짐
            log.error("Employee Service 호출 실패: employeeId={}", employeeId, e);
            throw new RuntimeException("직원 정보 확인 실패: " + e.getMessage());
        }
    }
}