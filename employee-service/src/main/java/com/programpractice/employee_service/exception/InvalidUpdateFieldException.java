package com.programpractice.employee_service.exception;

// 허용되지 않은 필드 수정 시도 시 발생하는 예외

public class InvalidUpdateFieldException extends RuntimeException {
    public InvalidUpdateFieldException(String message) {
        super(message);
    }
}
