package com.programpractice.approval_request_service.exception;

@org.springframework.web.bind.annotation.RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {
    
    @org.springframework.web.bind.annotation.ExceptionHandler(ApprovalRequestNotFoundException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleNotFound(
            ApprovalRequestNotFoundException e) {
        log.error("결재 요청을 찾을 수 없음: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("NOT_FOUND", e.getMessage());
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(error);
    }
    
    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidApprovalStepsException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleInvalidSteps(
            InvalidApprovalStepsException e) {
        log.error("잘못된 결재 단계: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_STEPS", e.getMessage());
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(error);
    }
    
    @org.springframework.web.bind.annotation.ExceptionHandler(
            org.springframework.web.bind.MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("유효성 검증 실패: {}", message);
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(error);
    }
    
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", e.getMessage());
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}

@lombok.Getter
@lombok.AllArgsConstructor
class ErrorResponse {
    private String code;
    private String message;
}