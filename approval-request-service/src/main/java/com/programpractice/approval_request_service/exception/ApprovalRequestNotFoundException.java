package com.programpractice.approval_request_service.exception;

// 결재 요청을 찾을 수 없는 예외
public class ApprovalRequestNotFoundException extends RuntimeException {
    public ApprovalRequestNotFoundException(String message) {
        super(message);
    }
}
