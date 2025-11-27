package com.programpractice.employee_service.dto;

import java.time.LocalDateTime;

import com.programpractice.employee_service.entity.Employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 직원 응답 DTO
@Getter
@AllArgsConstructor
@Builder
public class EmployeeResponse {
    
    private Long id;
    private String name;
    private String department;
    private String position;
    private LocalDateTime createdAt;
    
    public static EmployeeResponse from(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .name(employee.getName())
                .department(employee.getDepartment())
                .position(employee.getPosition())
                .createdAt(employee.getCreatedAt())
                .build();
    }
}
