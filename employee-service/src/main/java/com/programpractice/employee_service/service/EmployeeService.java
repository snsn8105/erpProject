package com.programpractice.employee_service.service;

import com.programpractice.employee_service.dto.EmployeeCreateRequest;
import com.programpractice.employee_service.dto.EmployeeCreateResponse;
import com.programpractice.employee_service.dto.EmployeeResponse;
import com.programpractice.employee_service.dto.EmployeeUpdateRequest;
import com.programpractice.employee_service.entity.Employee;
import com.programpractice.employee_service.exception.EmployeeNotFoundException;
import com.programpractice.employee_service.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    
    /**
     * 직원 생성
     * POST /employees
     */
    @Transactional
    public EmployeeCreateResponse createEmployee(EmployeeCreateRequest request) {
        log.info("직원 생성 요청: name={}, department={}, position={}", 
                request.getName(), request.getDepartment(), request.getPosition());
        
        Employee employee = request.toEntity();
        Employee savedEmployee = employeeRepository.save(employee);
        
        log.info("직원 생성 완료: id={}", savedEmployee.getId());
        return new EmployeeCreateResponse(savedEmployee.getId());
    }
    
    /**
     * 직원 목록 조회 (필터링 지원)
     * GET /employees?department=HR&position=Manager
     */
    public List<EmployeeResponse> getEmployees(String department, String position) {
        log.info("직원 목록 조회: department={}, position={}", department, position);
        
        List<Employee> employees;
        
        if (department != null && position != null) {
            employees = employeeRepository.findByDepartmentAndPosition(department, position);
        } else if (department != null) {
            employees = employeeRepository.findByDepartment(department);
        } else if (position != null) {
            employees = employeeRepository.findByPosition(position);
        } else {
            employees = employeeRepository.findAll();
        }
        
        log.info("직원 목록 조회 완료: {} 명", employees.size());
        return employees.stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 직원 상세 조회
     * GET /employees/{id}
     */
    public EmployeeResponse getEmployee(Long id) {
        log.info("직원 상세 조회: id={}", id);
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("직원을 찾을 수 없습니다: id=" + id));
        
        return EmployeeResponse.from(employee);
    }
    
    /**
     * 직원 수정 (department와 position만 수정 가능)
     * PUT /employees/{id}
     */
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        log.info("직원 수정 요청: id={}, department={}, position={}", 
                id, request.getDepartment(), request.getPosition());
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("직원을 찾을 수 없습니다: id=" + id));
        
        employee.updateDepartmentAndPosition(request.getDepartment(), request.getPosition());
        
        log.info("직원 수정 완료: id={}", id);
        return EmployeeResponse.from(employee);
    }
    
    /**
     * 직원 삭제
     * DELETE /employees/{id}
     */
    @Transactional
    public void deleteEmployee(Long id) {
        log.info("직원 삭제 요청: id={}", id);
        
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException("직원을 찾을 수 없습니다: id=" + id);
        }
        
        employeeRepository.deleteById(id);
        log.info("직원 삭제 완료: id={}", id);
    }
    
    /**
     * 직원 존재 여부 확인 (다른 서비스에서 호출)
     */
    public boolean existsById(Long id) {
        return employeeRepository.existsById(id);
    }
}