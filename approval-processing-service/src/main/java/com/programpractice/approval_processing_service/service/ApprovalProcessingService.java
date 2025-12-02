// approval-processing-service/src/main/java/com/programpractice/approval_processing_service/service/ApprovalProcessingService.java
package com.programpractice.approval_processing_service.service;

import com.programpractice.approval_processing_service.dto.*;
import com.programpractice.approval_processing_service.model.ApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalStep;
import com.programpractice.approval_processing_service.model.ApprovalStatus;
import com.programpractice.approval_processing_service.repository.InMemoryApprovalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        log.info("승인 요청 처리 시작: requestId={}, requesterId={}", 
                message.getRequestId(), message.getRequesterId());
        
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
                    .currentStepOrder(1)  // 초기값 설정
                    .build();
            
            // 3. 승인 단계 생성
            createApprovalSteps(approvalRequest, message.getSteps());
            
            // 4. 저장
            ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
            log.info("승인 요청 저장 완료: id={}, requestId={}", saved.getId(), saved.getRequestId());
            
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

        log.info("승인 단계 생성 완료: steps_count={}", stepRequests.size());
    }
    
    /**
     * 승인 처리 (REST API: POST /process/{approverId}/{requestId})
     */
    public ApprovalRequest processApproval(Long approverId, Integer requestId, 
                                               ProcessApprovalRequest request) {
        log.info("승인 처리 시작: approverId={}, requestId={}, status={}", 
                approverId, requestId, request.getStatus());
        
        // 1. 승인 요청 조회
        ApprovalRequest approvalRequest = approvalRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("승인 요청을 찾을 수 없습니다: " + requestId));
        
        // 2. 현재 단계 가져오기 (O(1) 접근 - stream 불필요!)
        ApprovalStep currentStep = approvalRequest.getCurrentStep();
        if (currentStep == null) {
            throw new IllegalStateException("처리할 승인 단계가 없습니다");
        }
        
        // 3. 승인자 확인
        if (!currentStep.getApproverId().equals(approverId)) {
            throw new IllegalArgumentException("승인 권한이 없습니다");
        }
        
        // 4. 승인/반려 처리
        if ("approved".equalsIgnoreCase(request.getStatus())) {
            currentStep.approve(request.getComment());
            log.info("단계 승인 완료: step={}", currentStep.getStep());
            
            // 5. 마지막 단계인지 확인
            if (approvalRequest.isLastStep()) {
                // 모든 단계 승인 완료
                approvalRequest.updateFinalStatus(ApprovalStatus.APPROVED);
                log.info("최종 승인 완료: requestId={}", requestId);
            } else {
                // 다음 단계로 포인터 이동
                approvalRequest.moveToNextStep();
                log.info("다음 단계로 이동: currentStepOrder={}", 
                        approvalRequest.getCurrentStepOrder());
            }
            
        } else if ("rejected".equalsIgnoreCase(request.getStatus())) {
            currentStep.reject(request.getComment());
            approvalRequest.updateFinalStatus(ApprovalStatus.REJECTED);
            log.info("승인 반려: step={}", currentStep.getStep());
        } else {
            throw new IllegalArgumentException("잘못된 상태값입니다: " + request.getStatus());
        }
        
        // 6. 저장
        approvalRequestRepository.save(approvalRequest);
        
        // 7. 결과 반환
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