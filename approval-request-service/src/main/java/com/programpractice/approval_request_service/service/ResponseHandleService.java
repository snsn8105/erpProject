package com.programpractice.approval_request_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.programpractice.approval_request_service.client.NotificationServiceClient;
import com.programpractice.approval_request_service.document.ApprovalRequest;
import com.programpractice.approval_request_service.document.Step;
import com.programpractice.approval_request_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_request_service.repository.ApprovalRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseHandleService {

    private final ApprovalRequestRepository repository;
    private final ApprovalMessagePublisher messagePublisher;
    private final NotificationServiceClient notificationClient;

    /**
     * 응답 메시지 처리 메인 로직
     */
    @Transactional
    public void handleResponse(ApprovalResponseMessage message) {
        // 1. 실패 응답 체크
        if (!message.isSuccess()) {
            log.error("승인 처리 실패 수신: requestId={}, error={}", message.getRequestId(), message.getErrorMessage());
            return;
        }

        // 2. DB 조회
        ApprovalRequest request = repository.findByRequestId(message.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("요청 찾기 실패: " + message.getRequestId()));

        // 3. 현재 단계 정보(상태, 코멘트, 시간) 업데이트 [핵심 수정 부분]
        Step currentStep = request.getCurrentStep();
        if (currentStep != null) {
            // 상태 업데이트
            currentStep.updateStatus(message.getStatus()); 
            
            log.info("단계 업데이트 완료: step={}, status={}, comment={}", 
                    currentStep.getStep(), message.getStatus(), message.getComment());
        }

        // 4. 승인/반려 분기 처리
        if ("approved".equalsIgnoreCase(message.getStatus())) {
            processApproval(request);
        } else if ("rejected".equalsIgnoreCase(message.getStatus())) {
            processRejection(request, message.getApproverId());
        }

        // 5. 변경사항 저장 (이 시점에 MongoDB에 반영됨)
        repository.save(request);
        log.info("승인 요청 저장 완료: requestId={}, finalStatus={}", request.getRequestId(), request.getFinalStatus());
    }

    // 승인 처리 내부 로직
    private void processApproval(ApprovalRequest request) {
        if (request.isLastStep()) {
            // 최종 승인
            request.updateFinalStatus("approved");
            log.info("최종 승인 확정");
            sendNotification(request, "approved", null);
        } else {
            // 다음 단계 이동
            request.moveToNextStep();
            log.info("다음 단계로 이동 (Current Step: {})", request.getCurrentStepOrder());
            
            // 다음 단계 진행을 위한 메시지 발행 (Publisher에게 위임)
            messagePublisher.publishNextStep(request); 
        }
    }

    // 반려 처리 내부 로직
    private void processRejection(ApprovalRequest request, Long rejectorId) {
        request.updateFinalStatus("rejected");
        log.info("승인 반려 확정");
        
        Integer rejectedBy = rejectorId != null ? rejectorId.intValue() : null;
        sendNotification(request, "rejected", rejectedBy);
    }

    // 알림 전송 헬퍼 메서드
    private void sendNotification(ApprovalRequest request, String status, Integer rejectedBy) {
        try {
            notificationClient.sendApprovalNotification(
                request.getRequestId(), request.getRequesterId(), request.getTitle(), status, rejectedBy
            );
        } catch (Exception e) {
            log.error("알림 전송 실패 (비즈니스 로직 계속 진행)", e);
        }
    }
}
