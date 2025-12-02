// approval-processing-service/src/main/java/com/programpractice/approval_processing_service/controller/ApprovalController.java
package com.programpractice.approval_processing_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.programpractice.approval_processing_service.dto.ApprovalDetailResponse;
import com.programpractice.approval_processing_service.dto.ProcessApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalStep;
import com.programpractice.approval_processing_service.service.ApprovalProcessingService;
import com.programpractice.approval_processing_service.service.ApprovalResponsePublisher;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 승인 처리 REST API Controller
 */
@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {
    
    private final ApprovalProcessingService processingService;
    private final ApprovalResponsePublisher responsePublisher;
    
    /**
     * GET /process/{approverId}
     * 결재자 대기 목록 조회
     */
    @GetMapping("/{approverId}")
    public ResponseEntity<java.util.List<ApprovalDetailResponse>> getApprovalList(
            @PathVariable Long approverId) {
        
        log.info("=== GET /process/{} 호출 ===", approverId);
        
        java.util.List<ApprovalDetailResponse> approvals = 
                processingService.getApprovalsByApproverId(approverId);
        
        log.info("승인 대기 목록 조회 완료: approverId={}, count={}", approverId, approvals.size());
        
        return ResponseEntity.ok(approvals);
    }
    
    /**
     * POST /process/{approverId}/{requestId}
     * 승인 또는 반려 처리
     */
    @PostMapping("/{approverId}/{requestId}")
    public ResponseEntity<ApprovalResponseDto> processApproval(
            @PathVariable Long approverId,
            @PathVariable Integer requestId,
            @Valid @RequestBody ProcessApprovalRequest request) {
        
        log.info("=== POST /process/{}/{} 호출 ===", approverId, requestId);
        log.info("요청 내용: status={}, comment={}", request.getStatus(), request.getComment());
        
        try {
            // 1. 승인 처리 (전체 ApprovalRequest 반환)
            ApprovalRequest result = processingService.processApproval(approverId, requestId, request);
            
            // 2. 처리된 단계 정보 추출
            int processedStepNumber = result.getCurrentStepOrder() - 1; // 방금 처리한 단계
            if ("rejected".equalsIgnoreCase(request.getStatus())) {
                // 반려된 경우 currentStepOrder가 증가하지 않으므로 그대로 사용
                processedStepNumber = result.getCurrentStepOrder();
            }
            
            ApprovalStep processedStep = null;
            if (processedStepNumber > 0 && processedStepNumber <= result.getSteps().size()) {
                processedStep = result.getSteps().get(processedStepNumber - 1);
            }
            
            if (processedStep != null) {
                log.info("✅ 승인 처리 완료: step={}, status={}, finalStatus={}", 
                        processedStep.getStep(), 
                        processedStep.getStatus(),
                        result.getFinalStatus());
            }
            
            // 3. RabbitMQ로 결과 발행
            log.info("RabbitMQ 메시지 발행 시작...");
            responsePublisher.publishApprovalResult(result);
            log.info("RabbitMQ 메시지 발행 완료");
            
            // 4. 클라이언트에 "received" 응답 반환
            ApprovalResponseDto response = ApprovalResponseDto.builder()
                    .status("received")
                    .build();
            
            log.info("=== API 응답 반환: status=received ===");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ 승인 처리 실패 (잘못된 요청): {}", e.getMessage());
            
            ApprovalResponseDto errorResponse = ApprovalResponseDto.builder()
                    .status("error")
                    .message(e.getMessage())
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ 승인 처리 중 예상치 못한 오류 발생", e);
            
            ApprovalResponseDto errorResponse = ApprovalResponseDto.builder()
                    .status("error")
                    .message("승인 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

/**
 * 승인 응답 DTO (REST API용)
 */
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class ApprovalResponseDto {
    private String status;   // "received", "error"
    private String message;  // 에러 메시지 (옵션)
}