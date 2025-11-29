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
    
    // 실제 승인 문서 업데이트 서비스 주입
    private final ApprovalRequestRepository approvalRequestRepository;
    
    /**
     * 승인 응답 메시지 수신 및 처리
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_RESPONSE_QUEUE)
    public void handleApprovalResponse(ApprovalResponseMessage message) {
        try {
            log.info("=== 승인 응답 메시지 수신 ===");
            log.info("approvalId: {}", message.getApprovalId());
            log.info("status: {}", message.getStatus());
            log.info("approverId: {}", message.getApproverId());
            log.info("success: " + message.isSuccess());
            
            if (!message.isSuccess()) {
                log.error("승인 처리 실패: approvalId={}, error={}", 
                        message.getApprovalId(), message.getErrorMessage());
                return;
            }

            // MongoDB 문서 업데이트 로직 구현
            ApprovalRequest approvalRequest = 
                    approvalRequestRepository.findById(message.getApprovalId())
                    .orElseThrow(() -> new IllegalArgumentException("승인 요청을 찾을 수 없습니다: " + message.getApprovalId()));
            
            // 단계별 상태 업데이트
            if (message.getStatus() != null && !message.getStatus().isEmpty()){
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

            approvalRequestRepository.save(approvalRequest);
            log.info("승인 응답 처리 완료: approvalId={}, finalStatus={}", 
                    message.getApprovalId(), approvalRequest.getFinalStatus());

        } catch (Exception e) {
            log.error("승인 응답 메시지 처리 중 오류 발생: approvalId={}", 
                    message.getApprovalId(), e);
            // TODO: 에러 처리 (DLQ로 전송 등)
        }
    }

    // 단계별 상태 업데이트
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