package com.programpractice.approval_processing_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.programpractice.approval_processing_service.dto.ApprovalDetailResponse;
import com.programpractice.approval_processing_service.dto.ApprovalResponseMessage;
import com.programpractice.approval_processing_service.dto.ProcessApprovalRequest;
import com.programpractice.approval_processing_service.dto.ReturnApprovalResult;
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
    public ResponseEntity<List<ApprovalDetailResponse>> getApprovalList(
            @PathVariable Long approverId) {
        
        log.info("GET /process/{} 호출", approverId);
        
        List<ApprovalDetailResponse> approvals = processingService.getApprovalsByApproverId(approverId);
        
        log.info("승인 대기 목록 조회 완료: approverId={}, count={}", approverId, approvals.size());
        
        return ResponseEntity.ok(approvals);
    }
    
    /**
     * POST /process/{approverId}/{requestId}
     * 승인 또는 반려 처리
     * 
     * Request Body:
     * {
     *   "status": "approved" 또는 "rejected",
     *   "comment": "코멘트 (선택)"
     * }
     * 
     * Response:
     * 1. pending 목록에 해당 approverId를 가진 인덱스가 대기 리스트에 수신 결과 전달
     * 2. 해당 approverId를 가진 인덱스가 수신 결과 정보 저장
     * 3. ApprovalResponse로 {"status": "received"} 반환
     */
    @PostMapping("/{approverId}/{requestId}")
    public ResponseEntity<ApprovalResponseDto> processApproval(
            @PathVariable Long approverId,
            @PathVariable String requestId,
            @Valid @RequestBody ProcessApprovalRequest request) {
        
        log.info("POST /process/{}/{} 호출", approverId, requestId);
        log.info("요청 내용: status={}, comment={}", request.getStatus(), request.getComment());
        
        try {
            // 1. 승인 처리
            ReturnApprovalResult result = processingService.processApproval(approverId, requestId, request);
            
            log.info("승인 처리 완료: step={}, status={}", result.getStep(), result.getStatus());
            
            // 2. RabbitMQ로 결과 발행
            ApprovalResponseMessage message = ApprovalResponseMessage.builder()
                    .approvalId(requestId)
                    .step(result.getStep())
                    .approverId(result.getApproverId())
                    .status(result.getStatus())
                    .finalStatus(result.getStatus()) // TODO: finalStatus 계산 로직
                    .comment(request.getComment())
                    .processedAt(result.getUpdatedAt())
                    .success(true)
                    .build();
            
            responsePublisher.publishApprovalResult(message);
            
            // 3. 클라이언트에 "received" 응답 반환
            ApprovalResponseDto response = ApprovalResponseDto.builder()
                    .status("received")
                    .build();
            
            log.info("승인 처리 API 응답: status=received");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("승인 처리 실패 (잘못된 요청): {}", e.getMessage());
            
            ApprovalResponseDto errorResponse = ApprovalResponseDto.builder()
                    .status("error")
                    .message(e.getMessage())
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("승인 처리 중 오류 발생", e);
            
            ApprovalResponseDto errorResponse = ApprovalResponseDto.builder()
                    .status("error")
                    .message("승인 처리 중 오류가 발생했습니다")
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
