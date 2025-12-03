package com.programpractice.approval_processing_service.service;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.programpractice.approval_processing_service.config.RabbitMQConfig;
import com.programpractice.approval_processing_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_processing_service.model.ApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponsePublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 승인 처리 결과 발행
     * * 변경 사항:
     * - 중간 단계이든 최종 단계이든 무조건 Response Queue로 결과를 보냅니다.
     * - Request Service가 이 메시지를 받아 MongoDB를 업데이트하고, 
     * 다음 단계가 있다면 Request Service가 다시 Request Queue로 메시지를 쏘게 됩니다.
     */
    public void publishApprovalResult(ApprovalRequest approvalRequest) {
        try {
            log.info("=== 메시지 발행 시작 ===");
            log.info("requestId={}, finalStatus={}, currentStep={}/{}", 
                    approvalRequest.getRequestId(),
                    approvalRequest.getFinalStatus(),
                    approvalRequest.getCurrentStepOrder(),
                    approvalRequest.getSteps().size());

            // 1. 응답 메시지 생성
            ApprovalResponseMessage message = createResponseMessage(approvalRequest);
            
            // 2. Response Queue로 발행 (Request Service가 수신)
            log.info("발행 대상: Exchange={}, RoutingKey={}", 
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY);
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.APPROVAL_EXCHANGE,
                    RabbitMQConfig.APPROVAL_RESPONSE_ROUTING_KEY,
                    message
            );
            
            log.info("✅ ResponseMessage 발행 완료: status={}, step={}", 
                    message.getStatus(), message.getStep());
            
        } catch (Exception e) {
            log.error("=== 메시지 발행 실패 ===", e);
            log.error("requestId={}", approvalRequest.getRequestId());
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }

    /**
     * 응답 메시지 생성
     */
    private ApprovalResponseMessage createResponseMessage(ApprovalRequest request) {
        // 처리된 단계 번호 계산
        // 만약 상태가 PENDING(진행중)이라면, moveToNextStep()이 이미 호출되어 currentStepOrder가 증가된 상태임
        // 따라서 방금 처리된 단계는 currentStepOrder - 1임
        int processedStep;
        String status;

        if (request.getFinalStatus() == ApprovalStatus.REJECTED) {
            // 반려됨
            processedStep = request.getCurrentStepOrder(); // 반려는 단계 이동 안함
            status = "rejected";
        } else if (request.getFinalStatus() == ApprovalStatus.APPROVED) {
            // 최종 승인됨
            processedStep = request.getCurrentStepOrder(); // 마지막 단계
            status = "approved";
        } else {
            // 진행 중 (중간 단계 승인)
            processedStep = request.getCurrentStepOrder() - 1; // 이미 다음 단계로 포인터가 넘어감
            status = "approved"; // 중간 단계 승인이므로 상태는 approved
        }

        // 해당 단계의 승인자 정보 찾기
        // steps 리스트는 0부터 시작하므로 index는 processedStep - 1
        Long approverId = null;
        String approverName = ""; // 필요시 추가 구현
        String comment = "";
        
        if (processedStep > 0 && processedStep <= request.getSteps().size()) {
            var stepObj = request.getSteps().get(processedStep - 1);
            approverId = stepObj.getApproverId();
            comment = stepObj.getComment();
        }

        return ApprovalResponseMessage.builder()
                .id(request.getId())
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId().intValue())
                .title(request.getTitle())
                .step(processedStep)          // 처리된 단계 번호
                .approverId(approverId)       // 승인자 ID
                .status(status)               // 단계 상태 (approved/rejected)
                .comment(comment)             // 코멘트
                .finalStatus(request.getFinalStatus().name()) // 최종 상태
                .updatedAt(LocalDateTime.now())
                .processedAt(request.getUpdatedAt())
                .success(true)
                .build();
    }
}
