package com.programpractice.approval_processing_service.service;

import org.springframework.stereotype.Service;

import com.programpractice.approval_processing_service.client.EmployeeServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 직원 검증 서비스
 * Employee Service를 호출하여 직원 존재 여부 확인
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeValidationService {
    
    private final EmployeeServiceClient employeeServiceClient;
    
    /**
     * 직원 존재 여부 검증
     * 
     * @param employeeId 직원 ID
     * @throws IllegalArgumentException 직원이 존재하지 않는 경우
     */
    public void validateEmployee(Long employeeId) {
        log.info("=== 직원 검증 시작: employeeId={} ===", employeeId);
        
        if (employeeId == null) {
            log.error("❌ 직원 ID가 null입니다");
            throw new IllegalArgumentException("직원 ID는 필수입니다");
        }
        
        try {
            // Employee Service 호출
            boolean exists = employeeServiceClient.existsEmployee(employeeId);
            
            if (!exists) {
                log.error("❌ 직원이 존재하지 않음: employeeId={}", employeeId);
                throw new IllegalArgumentException(
                        "존재하지 않는 직원입니다: employeeId=" + employeeId);
            }
            
            log.info("✅ 직원 검증 완료: employeeId={}", employeeId);
            
        } catch (IllegalArgumentException e) {
            // 직원이 존재하지 않는 경우 - 그대로 throw
            throw e;
            
        } catch (Exception e) {
            // Employee Service 호출 실패 - 상세 로그 출력
            log.error("❌ Employee Service 호출 중 오류 발생: employeeId={}", 
                    employeeId, e);
            throw new RuntimeException(
                    "직원 정보 확인 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 여러 직원의 존재 여부 일괄 검증
     * 
     * @param employeeIds 직원 ID 목록
     * @throws IllegalArgumentException 존재하지 않는 직원이 있는 경우
     */
    public void validateEmployees(Long... employeeIds) {
        log.info("=== 직원 일괄 검증 시작: count={} ===", employeeIds.length);
        
        for (Long employeeId : employeeIds) {
            validateEmployee(employeeId);
        }
        
        log.info("✅ 직원 일괄 검증 완료: count={}", employeeIds.length);
    }
}