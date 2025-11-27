package com.programpractice.employee_service.exception;

// 직원을 찾을 수 없을 때 발생하는 예외

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
