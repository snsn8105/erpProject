package com.programpractice.approval_processing_service.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 직원 검증 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeValidationService {
    
    // TODO: Employee Service와 통신 (RestTemplate or WebClient)
    // private final RestTemplate restTemplate;
    
    public void validateEmployee(Long employeeId) {
        log.debug("직원 검증: employeeId={}", employeeId);
        
        // TODO: Employee Service 호출
        // String url = "http://localhost:8081/employees/" + employeeId + "/exists";
        // Boolean exists = restTemplate.getForObject(url, Boolean.class);
        // if (!Boolean.TRUE.equals(exists)) {
        //     throw new IllegalArgumentException("존재하지 않는 직원입니다: " + employeeId);
        // }
        
        // 임시로 검증 통과
        log.debug("직원 검증 완료: employeeId={}", employeeId);
    }
}
