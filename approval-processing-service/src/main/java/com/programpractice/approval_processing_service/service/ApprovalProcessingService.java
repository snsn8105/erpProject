// approval-processing-service/src/main/java/com/programpractice/approval_processing_service/service/ApprovalProcessingService.java
package com.programpractice.approval_processing_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.programpractice.approval_processing_service.dto.ApprovalDetailResponse;
import com.programpractice.approval_processing_service.dto.ApprovalRequestMessage;
import com.programpractice.approval_processing_service.dto.ApprovalStepDto;
import com.programpractice.approval_processing_service.dto.ProcessApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalStatus;
import com.programpractice.approval_processing_service.model.ApprovalStep;
import com.programpractice.approval_processing_service.repository.InMemoryApprovalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApprovalProcessingService {
    
    private final InMemoryApprovalRepository approvalRequestRepository;
    private final EmployeeValidationService employeeValidationService;
    
    /**
     * 승인 요청 초기 처리 (RabbitMQ 메시지 수신 시)
     */
    public ApprovalRequest processApprovalRequest(ApprovalRequestMessage message) {
        log.info("=== 승인 요청 초기 처리 시작 ===");
        log.info("requestId={}, requesterId={}, title={}", 
                message.getRequestId(), message.getRequesterId(), message.getTitle());
        
        try {
            // 1. 요청자 검증
            employeeValidationService.validateEmployee(message.getRequesterId());
            
            // 2. 승인 요청 생성
            ApprovalRequest approvalRequest = ApprovalRequest.builder()
                    .id(message.getId())
                    .requestId(message.getRequestId())
                    .requesterId(message.getRequesterId())
                    .title(message.getTitle())
                    .content(message.getContent())
                    .currentStepOrder(1)  // 초기값: 1단계부터 시작
                    .build();
            
            // 3. 승인 단계 생성
            createApprovalSteps(approvalRequest, message.getSteps());
            
            // 4. 저장
            ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
            
            log.info("승인 요청 저장 완료: id={}, requestId={}, 총 단계 수={}, 현재 단계={}", 
                    saved.getId(), saved.getRequestId(), saved.getSteps().size(), saved.getCurrentStepOrder());
            
            // 5. 첫 번째 승인자 정보 로깅
            ApprovalStep firstStep = saved.getCurrentStep();
            if (firstStep != null) {
                log.info("첫 번째 승인자: approverId={}, step={}", 
                        firstStep.getApproverId(), firstStep.getStep());
            }
            
            return saved;
            
        } catch (Exception e) {
            log.error("승인 요청 처리 실패: requestId={}", message.getRequestId(), e);
            throw new RuntimeException("승인 요청 처리 실패", e);
        }
    }
    
    /**
     * 승인 단계 생성
     */
    private void createApprovalSteps(ApprovalRequest approvalRequest, 
                                     List<ApprovalRequestMessage.ApprovalStepDto> stepRequests) {
        
        if (stepRequests == null || stepRequests.isEmpty()) {
            throw new IllegalArgumentException("승인 단계 정보가 비어있습니다.");
        }

        for (ApprovalRequestMessage.ApprovalStepDto stepReq : stepRequests) {
            ApprovalStep step = ApprovalStep.builder()
                    .step(stepReq.getStep())
                    .approverId(stepReq.getApproverId())
                    .status(ApprovalStatus.PENDING)
                    .build();
            
            approvalRequest.addStep(step);
        }

        log.info("승인 단계 생성 완료: 총 {}개 단계", stepRequests.size());
    }
    
    /**
     * 승인 처리 (REST API: POST /process/{approverId}/{requestId})
     * 
     * 처리 흐름:
     * 1. 현재 단계 조회 (currentStepOrder를 통해 O(1) 접근)
     * 2. 승인자 권한 확인
     * 3. 승인/반려 처리
     * 4. 마지막 단계인지 확인
     *    - 마지막이면: finalStatus를 APPROVED/REJECTED로 변경
     *    - 아니면: currentStepOrder를 다음 단계로 이동 (moveToNextStep)
     * 5. 저장 및 반환
     */
    public ApprovalRequest processApproval(Long approverId, Integer requestId, 
                                          ProcessApprovalRequest request) {
        log.info("=== 승인 처리 시작 ===");
        log.info("approverId={}, requestId={}, 요청 상태={}", 
                approverId, requestId, request.getStatus());
        
        // 1. 승인 요청 조회
        ApprovalRequest approvalRequest = approvalRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> {
                    log.error("승인 요청을 찾을 수 없음: requestId={}", requestId);
                    return new IllegalArgumentException("승인 요청을 찾을 수 없습니다: " + requestId);
                });
        
        log.info("승인 요청 조회 완료: 현재 단계={}/{}, finalStatus={}", 
                approvalRequest.getCurrentStepOrder(), 
                approvalRequest.getSteps().size(),
                approvalRequest.getFinalStatus());
        
        // 2. ⭐ 현재 단계 가져오기 (O(1) 접근)
        ApprovalStep currentStep = approvalRequest.getCurrentStep();
        if (currentStep == null) {
            log.error("처리할 승인 단계가 없음: requestId={}, currentStepOrder={}", 
                    requestId, approvalRequest.getCurrentStepOrder());
            throw new IllegalStateException("처리할 승인 단계가 없습니다");
        }
        
        log.info("현재 처리 대상 단계: step={}, approverId={}, status={}", 
                currentStep.getStep(), currentStep.getApproverId(), currentStep.getStatus());
        
        // 3. 승인자 확인
        if (!currentStep.getApproverId().equals(approverId)) {
            log.error("승인 권한 없음: 요청한 approverId={}, 실제 approverId={}", 
                    approverId, currentStep.getApproverId());
            throw new IllegalArgumentException(
                    String.format("승인 권한이 없습니다 (요청: %d, 필요: %d)", 
                            approverId, currentStep.getApproverId()));
        }
        
        // 4. 승인/반려 처리
        if ("approved".equalsIgnoreCase(request.getStatus())) {
            
            // 4-1. 현재 단계 승인 처리
            currentStep.approve(request.getComment());
            log.info("✅ 단계 승인 완료: step={}, approverId={}, comment={}", 
                    currentStep.getStep(), approverId, request.getComment());
            
            // 4-2.  마지막 단계인지 확인 (승인 처리 후!)
            if (approvalRequest.isLastStep()) {
                // 모든 단계 승인 완료
                approvalRequest.updateFinalStatus(ApprovalStatus.APPROVED);
                log.info("🎉 최종 승인 완료: requestId={}, 모든 {}개 단계 승인됨", 
                        requestId, approvalRequest.getSteps().size());
            } else {
                // 다음 단계로 포인터 이동
                approvalRequest.moveToNextStep();
                log.info("➡️ 다음 단계로 이동: 현재 단계 {} -> 다음 단계 {}", 
                        currentStep.getStep(), approvalRequest.getCurrentStepOrder());
                
                // 다음 단계 정보 로깅
                ApprovalStep nextStep = approvalRequest.getCurrentStep();
                if (nextStep != null) {
                    log.info("다음 승인자 정보: step={}, approverId={}", 
                            nextStep.getStep(), nextStep.getApproverId());
                }
            }
            
        } else if ("rejected".equalsIgnoreCase(request.getStatus())) {
            
            // 반려 처리
            currentStep.reject(request.getComment());
            approvalRequest.updateFinalStatus(ApprovalStatus.REJECTED);
            
            log.info("❌ 승인 반려: step={}, approverId={}, comment={}", 
                    currentStep.getStep(), approverId, request.getComment());
            
        } else {
            log.error("잘못된 상태값: {}", request.getStatus());
            throw new IllegalArgumentException("잘못된 상태값입니다: " + request.getStatus());
        }
        
        // 6. 저장
        approvalRequestRepository.save(approvalRequest);
        
        log.info("=== 승인 처리 완료 ===");
        log.info("최종 상태: finalStatus={}, currentStepOrder={}/{}", 
                approvalRequest.getFinalStatus(), 
                approvalRequest.getCurrentStepOrder(),
                approvalRequest.getSteps().size());
        
        // 7. 전체 ApprovalRequest 반환 (Controller에서 필요한 정보 추출)
        return approvalRequest;
    }
    
    /**
     * 승인 요청 상세 조회
     */
    @Transactional(readOnly = true)
    public List<ApprovalDetailResponse> getApprovalsByApproverId(Long approverId) {
        log.info("승인자별 승인 목록 조회: approverId={}", approverId);
        
        List<ApprovalRequest> approvals = approvalRequestRepository
                .findPendingApprovalsByApproverId(approverId);
        
        log.info("승인 대기 목록 조회 완료: approverId={}, 대기 건수={}", 
                approverId, approvals.size());
        
        return approvals.stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Entity -> DTO 변환
     */
    private ApprovalDetailResponse toDetailResponse(ApprovalRequest entity) {
        List<ApprovalStepDto> stepDtos = entity.getSteps().stream()
                .map(step -> ApprovalStepDto.builder()
                        .step(step.getStep())
                        .approverId(step.getApproverId())
                        .status(step.getStatus().name().toLowerCase())
                        .comment(step.getComment())
                        .processedAt(step.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ApprovalDetailResponse.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .requesterId(entity.getRequesterId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .finalStatus(entity.getFinalStatus().name().toLowerCase())
                .steps(stepDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}