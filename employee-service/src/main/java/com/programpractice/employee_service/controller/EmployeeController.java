package com.programpractice.employee_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.programpractice.employee_service.dto.EmployeeCreateRequest;
import com.programpractice.employee_service.dto.EmployeeCreateResponse;
import com.programpractice.employee_service.dto.EmployeeResponse;
import com.programpractice.employee_service.dto.EmployeeUpdateRequest;
import com.programpractice.employee_service.service.EmployeeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {
    
    private final EmployeeService employeeService;

    // POST /employees
    // 직원 생성
    @PostMapping
    public ResponseEntity<EmployeeCreateResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        
        log.info("POST /employees 호출");
        EmployeeCreateResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // GET /employees
    // 직원 목록 조회 (필터링 지원)
    // 예: /employees?department=HR&position=Manager
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position) {
        
        log.info("GET /employees 호출: department={}, position={}", department, position);
        List<EmployeeResponse> employees = employeeService.getEmployees(department, position);
        return ResponseEntity.ok(employees);
    }
    
    // GET /employees/{id}
    // 직원 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        log.info("GET /employees/{} 호출", id);
        EmployeeResponse employee = employeeService.getEmployee(id);
        return ResponseEntity.ok(employee);
    }
    
    // PUT /employees/{id}
    // 직원 수정 (department와 position만 수정 가능)
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        
        log.info("PUT /employees/{} 호출", id);
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(response);
    }
    
    // DELETE /employees/{id}
    // 직원 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        log.info("DELETE /employees/{} 호출", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
    
    /*
     * GET /employees/{id}/exists
     * 직원 존재 여부 확인 (다른 서비스에서 호출)
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> existsEmployee(@PathVariable Long id) {
        log.info("GET /employees/{}/exists 호출", id);
        boolean exists = employeeService.existsById(id);
        return ResponseEntity.ok(exists);
    }
}
