package com.programpractice.approval_request_service.exception;

// 결재 단계가 잘못된 예외
public class InvalidApprovalStepsException extends RuntimeException {
    public InvalidApprovalStepsException(String message) {
        super(message);
    }
}
