package com.programpractice.approval_request_service.listener;

import com.programpractice.approval_request_service.config.RabbitMQConfig;
import com.programpractice.approval_request_service.dto.ApprovalResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 승인 응답 메시지 수신 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalResponseListener {
    
    // TODO: 실제 승인 문서 업데이트 서비스 주입
    // private final ApprovalDocumentService approvalDocumentService;
    
    /**
     * 승인 응답 메시지 수신 및 처리
     */
    @RabbitListener(queues = RabbitMQConfig.APPROVAL_RESPONSE_QUEUE)
    public void handleApprovalResponse(ApprovalResponseMessage message) {
        try {
            log.info("승인 응답 메시지 수신: approvalId={}, status={}", 
                    message.getApprovalId(), message.getStatus());
            
            if (message.isSuccess()) {
                // 성공 시 문서 상태 업데이트
                log.info("승인 처리 성공: approvalId={}, status={}", 
                        message.getApprovalId(), message.getStatus());
                
                // TODO: MongoDB 문서 상태 업데이트
                // approvalDocumentService.updateStatus(
                //     message.getApprovalId(), 
                //     message.getStatus(),
                //     message.getApproverId(),
                //     message.getComment()
                // );
                
            } else {
                // 실패 시 에러 로깅
                log.error("승인 처리 실패: approvalId={}, error={}", 
                        message.getApprovalId(), message.getErrorMessage());
                
                // TODO: 에러 처리 로직 (재시도, 알림 등)
            }
            
        } catch (Exception e) {
            log.error("승인 응답 메시지 처리 중 오류 발생: approvalId={}", 
                    message.getApprovalId(), e);
            // TODO: 에러 처리 (DLQ로 전송 등)
        }
    }
}