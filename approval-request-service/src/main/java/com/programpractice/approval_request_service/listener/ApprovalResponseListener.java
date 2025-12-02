// approval-request-service/src/main/java/com/programpractice/approval_request_service/listener/ApprovalResponseListener.java
package com.programpractice.approval_request_service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.programpractice.approval_request_service.config.RabbitMQConfig;
import com.programpractice.approval_request_service.document.ApprovalRequest;
import com.programpractice.approval_request_service.document.Step;
import com.programpractice.approval_request_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_request_service.repository.ApprovalRequestRepository;
import com.programpractice.approval_request_service.service.ApprovalMessagePublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponseListener {
    
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalMessagePublisher messagePublisher;
    
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_RESPONSE_QUEUE)
    public void handleApprovalResponse(ApprovalResponseMessage message) {
        try {
            log.info("=== 승인 응답 메시지 수신 ===");
            log.info("requestId: {}", message.getRequestId());
            log.info("status: {}", message.getStatus());
            log.info("approverId: {}", message.getApproverId());
            
            if (!message.isSuccess()) {
                log.error("승인 처리 실패: requestId={}, error={}", 
                        message.getRequestId(), message.getErrorMessage());
                return;
            }
            
            // MongoDB 문서 조회
            ApprovalRequest approvalRequest = approvalRequestRepository.findByRequestId(message.getRequestId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "승인 요청을 찾을 수 없습니다: " + message.getRequestId()));
            
            // ⭐ 현재 단계의 상태 업데이트
            Step currentStep = approvalRequest.getCurrentStep();
            if (currentStep != null) {
                currentStep.updateStatus(message.getStatus());
                log.info("단계 상태 업데이트: step={}, status={}", 
                        currentStep.getStep(), message.getStatus());
            }
            
            // ⭐ 승인/반려 처리
            if ("approved".equalsIgnoreCase(message.getStatus())) {
                
                // 마지막 단계인지 확인
                if (approvalRequest.isLastStep()) {
                    // 모든 단계 완료
                    approvalRequest.updateFinalStatus("approved");
                    log.info("최종 승인 완료: requestId={}", approvalRequest.getRequestId());
                    
                    // TODO: Notification Service 호출
                    
                } else {
                    // 다음 단계로 이동
                    approvalRequest.moveToNextStep();
                    log.info("다음 단계로 이동: requestId={}, currentStep={}", 
                            approvalRequest.getRequestId(), approvalRequest.getCurrentStepOrder());
                    
                    // 다음 단계를 Processing Service로 전송
                    sendNextStepToProcessingService(approvalRequest);
                }
                
            } else if ("rejected".equalsIgnoreCase(message.getStatus())) {
                approvalRequest.updateFinalStatus("rejected");
                log.info("승인 반려: requestId={}", approvalRequest.getRequestId());
                
                // TODO: Notification Service 호출
            }
            
            // 저장
            approvalRequestRepository.save(approvalRequest);
            
            log.info("승인 응답 처리 완료: requestId={}, finalStatus={}", 
                    message.getRequestId(), approvalRequest.getFinalStatus());
            
        } catch (Exception e) {
            log.error("승인 응답 메시지 처리 중 오류 발생: requestId={}", 
                    message.getRequestId(), e);
        }
    }
    
    /**
     * ⭐ 다음 단계를 Processing Service로 전송
     */
    private void sendNextStepToProcessingService(ApprovalRequest approvalRequest) {
        try {
            // 다음 단계 정보 구성
            com.programpractice.approval_request_service.dto.ApprovalRequestMessage.ApprovalStepDto nextStepDto = 
                com.programpractice.approval_request_service.dto.ApprovalRequestMessage.ApprovalStepDto.builder()
                    .step(approvalRequest.getCurrentStepOrder())
                    .approverId(approvalRequest.getCurrentStep().getApproverId().longValue())
                    .build();
            
            // 메시지 생성
            com.programpractice.approval_request_service.dto.ApprovalRequestMessage message = 
                com.programpractice.approval_request_service.dto.ApprovalRequestMessage.builder()
                    .id(approvalRequest.getId())
                    .requestId(approvalRequest.getRequestId())
                    .requesterId(approvalRequest.getRequesterId().longValue())
                    .title(approvalRequest.getTitle())
                    .content(approvalRequest.getContent())
                    .steps(java.util.List.of(nextStepDto))  // 다음 단계만 전송
                    .requestedAt(approvalRequest.getCreatedAt())
                    .build();
            
            // 발행
            messagePublisher.publishApprovalRequest(message);
            
            log.info("다음 단계 메시지 발행 완료: requestId={}, step={}", 
                    approvalRequest.getRequestId(), approvalRequest.getCurrentStepOrder());
            
        } catch (Exception e) {
            log.error("다음 단계 메시지 발행 실패", e);
        }
    }
}
