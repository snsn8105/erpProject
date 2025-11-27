package com.programpractice.employee_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// (부서, 직책) 수정 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeUpdateRequest {
    
    @NotBlank(message = "부서는 필수입니다")
    @Size(max = 15, message = "부서는 15자를 초과할 수 없습니다")
    private String department;
    
    @NotBlank(message = "직책은 필수입니다")
    @Size(max = 20, message = "직책은 20자를 초과할 수 없습니다")
    private String position;
}
