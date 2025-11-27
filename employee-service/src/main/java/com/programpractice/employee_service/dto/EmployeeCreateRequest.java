package com.programpractice.employee_service.dto;

import java.time.LocalDateTime;

import com.programpractice.employee_service.entity.Employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 직원 생성 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCreateRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 30, message = "이름은 30자를 초과할 수 없습니다")
    private String name;

    @NotBlank(message = "부서는 필수입니다")
    @Size(max = 15, message = "부서는 15자를 초과할 수 없습니다")
    private String department;

    @NotBlank(message = "직책은 필수입니다.")
    @Size(max = 20, message = "직책은 20자를  초과할 수 없습니다")
    private String position;

    public Employee toEntity() {
        return Employee.builder()
                .name(name)
                .department(department)
                .position(position)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
