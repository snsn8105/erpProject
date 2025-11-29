package com.programpractice.approval_request_service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.programpractice.approval_request_service.config.RabbitMQConfig;
import com.programpractice.approval_request_service.document.ApprovalRequest;
import com.programpractice.approval_request_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_request_service.repository.ApprovalRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 승인 응답 메시지 수신 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponseListener {
    
    private final ApprovalRequestRepository approvalRequestRepository;
    
    /**
     * 승인 응답 메시지 수신 및 처리
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_RESPONSE_QUEUE)
    public void handleApprovalResponse(ApprovalResponseMessage message) {
        try {
            log.info("=== 승인 응답 메시지 수신 ===");
            log.info("requestId: {}", message.getRequestId());
            log.info("status: {}", message.getStatus());
            log.info("approverId: {}", message.getApproverId());
            log.info("success: {}", message.isSuccess());
            
            if (!message.isSuccess()) {
                log.error("승인 처리 실패: requestId={}, error={}", 
                        message.getRequestId(), message.getErrorMessage());
                return;
            }
            
            // MongoDB 문서 조회
            ApprovalRequest approvalRequest = approvalRequestRepository.findById(message.getRequestId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "승인 요청을 찾을 수 없습니다: " + message.getRequestId()));
            
            log.info("MongoDB 문서 조회 완료: requestId={}", approvalRequest.getRequestId());
            
            // 단계별 상태 업데이트
            if (message.getStatus() != null && !message.getStatus().isEmpty()) {
                updateStepStatus(approvalRequest, message);
            }
            
            // 최종 상태 업데이트 (모든 단계 완료 또는 반려 시)
            if ("approved".equals(message.getStatus()) && approvalRequest.areAllStepsApproved()) {
                approvalRequest.updateFinalStatus("approved");
                log.info("모든 단계 승인 완료: requestId={}", approvalRequest.getRequestId());
            } else if ("rejected".equals(message.getStatus())) {
                approvalRequest.updateFinalStatus("rejected");
                log.info("승인 반려: requestId={}", approvalRequest.getRequestId());
            }
            
            // 저장
            approvalRequestRepository.save(approvalRequest);
            
            log.info("승인 응답 처리 완료: requestId={}, finalStatus={}", 
                    message.getRequestId(), approvalRequest.getFinalStatus());
            
        } catch (Exception e) {
            log.error("승인 응답 메시지 처리 중 오류 발생: requestId={}", 
                    message.getRequestId(), e);
            // TODO: DLQ로 전송 또는 재시도 로직
        }
    }
    
    /**
     * 단계별 상태 업데이트
     */
    private void updateStepStatus(ApprovalRequest approvalRequest, ApprovalResponseMessage message) {
        try {
            // approverId로 해당 단계 찾기
            approvalRequest.getSteps().stream()
                    .filter(step -> step.getApproverId().equals(message.getApproverId().intValue()))
                    .findFirst()
                    .ifPresent(step -> {
                        step.updateStatus(message.getStatus());
                        log.info("단계 상태 업데이트: step={}, approverId={}, status={}", 
                                step.getStep(), message.getApproverId(), message.getStatus());
                    });
        } catch (Exception e) {
            log.error("단계 상태 업데이트 실패", e);
        }
    }
}
