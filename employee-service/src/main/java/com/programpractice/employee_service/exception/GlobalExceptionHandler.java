package com.programpractice.employee_service.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


// 전역 예외 처리기
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    
    // 직원을 찾을 수 없는 경우
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException e) {
        log.error("직원을 찾을 수 없음: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    
    // 유효성 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("유효성 검증 실패: {}", message);
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    
    // 허용되지 않은 필드 수정 시도
    @ExceptionHandler(InvalidUpdateFieldException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUpdateField(InvalidUpdateFieldException e) {
        log.error("허용되지 않은 필드 수정 시도: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_FIELD", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    
    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}


// 에러 응답 DTO
 
@Getter
@AllArgsConstructor
class ErrorResponse {
    private String code;
    private String message;
}
