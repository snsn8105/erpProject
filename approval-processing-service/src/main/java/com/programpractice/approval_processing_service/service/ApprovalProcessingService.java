package com.programpractice.approval_processing_service.service;

import com.programpractice.approval_processing_service.dto.*;
import com.programpractice.approval_processing_service.entity.ApprovalRequest;
import com.programpractice.approval_processing_service.entity.ApprovalStep;
import com.programpractice.approval_processing_service.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 승인 처리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApprovalProcessingService {
    
    private final ApprovalRequestRepository approvalRequestRepository;
    private final EmployeeValidationService employeeValidationService;
    
    /**
     * 승인 요청 초기 처리 (RabbitMQ 메시지 수신 시)
     * - DB에 승인 요청 저장
     * - 승인 단계 생성 (예: 2단계 승인)
     */
    public ApprovalResponseMessage processApprovalRequest(ApprovalRequestMessage message) {
        log.info("승인 요청 처리 시작: requestId={}, requesterId={}", 
                message.getRequestId(), message.getRequesterId());
        
        try {
            // 1. 요청자 검증
            employeeValidationService.validateEmployee(message.getRequesterId());
            
            // 2. 승인 요청 생성
            ApprovalRequest approvalRequest = ApprovalRequest.builder()
                    .requestId(message.getRequestId())  // MongoDB ObjectId 저장
                    .requesterId(message.getRequesterId())
                    .title(message.getTitle())
                    .content(message.getContent())
                    .build();
            
            // 3. 승인 단계 생성 (예시: 2단계 승인)
            createApprovalSteps(approvalRequest, message.getSteps());
            
            // 4. 저장
            ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
            log.info("승인 요청 저장 완료: id={}, requestId={}", saved.getId(), saved.getRequestId());
            
            // 5. 첫 번째 승인자 정보 가져오기
            ApprovalStep firstStep = saved.getSteps().get(0);
            
            // 6. 응답 메시지 생성
            ApprovalResponseMessage response = ApprovalResponseMessage.builder()
                    .requestId(message.getRequestId())
                    .status("pending")
                    .approverId(firstStep.getApproverId())
                    .approverName("Approver_" + firstStep.getApproverId())
                    .comment("승인 요청이 등록되었습니다")
                    .processedAt(LocalDateTime.now())
                    .success(true)
                    .build();
            
            log.info("승인 요청 처리 완료: requestId={}", message.getRequestId());
            return response;
            
        } catch (Exception e) {
            log.error("승인 요청 처리 실패: requestId={}", message.getRequestId(), e);
            
            return ApprovalResponseMessage.builder()
                    .requestId(message.getRequestId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * 승인 단계 생성 (입력받은 정보 기반)
     */
    private void createApprovalSteps(ApprovalRequest approvalRequest, List<ApprovalRequestMessage.ApprovalStepDto> stepRequests) {
        
        // 유효성 검사: 단계 정보가 없으면 예외 처리
        if (stepRequests == null || stepRequests.isEmpty()) {
            throw new IllegalArgumentException("승인 단계 정보가 비어있습니다.");
        }

        // 입력받은 리스트를 순회하며 Entity 생성
        for (ApprovalRequestMessage.ApprovalStepDto stepReq : stepRequests) {
            ApprovalStep step = ApprovalStep.builder()
                    .step(stepReq.getStep())
                    .approverId(stepReq.getApproverId())
                    .status(com.programpractice.approval_processing_service.entity.ApprovalStatus.PENDING) // 초기 상태
                    .build();
            
            approvalRequest.addStep(step);
        }

        log.info("승인 단계 생성 완료: steps_count={}", stepRequests.size());
    }
    
    /**
     * 승인 처리 (REST API: POST /process/{approverId}/{requestId})
     */
    public ReturnApprovalResult processApproval(Long approverId, String requestId, ProcessApprovalRequest request) {
        log.info("승인 처리 시작: approverId={}, requestId={}, status={}", 
                approverId, requestId, request.getStatus());
        
        // 1. 승인 요청 조회
        ApprovalRequest approvalRequest = approvalRequestRepository.findByRequestIdWithSteps(requestId)
                .orElseThrow(() -> new IllegalArgumentException("승인 요청을 찾을 수 없습니다: " + requestId));
        
        // 2. 다음 PENDING 단계 찾기
        ApprovalStep currentStep = approvalRequest.getNextPendingStep();
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
            
            // 5. 다음 단계 확인
            ApprovalStep nextStep = approvalRequest.getNextPendingStep();
            if (nextStep == null) {
                // 모든 단계 승인 완료
                approvalRequest.updateFinalStatus(
                    com.programpractice.approval_processing_service.entity.ApprovalStatus.APPROVED
                );
                log.info("최종 승인 완료: requestId={}", requestId);
            }
            
        } else if ("rejected".equalsIgnoreCase(request.getStatus())) {
            currentStep.reject(request.getComment());
            approvalRequest.updateFinalStatus(
                com.programpractice.approval_processing_service.entity.ApprovalStatus.REJECTED
            );
            log.info("승인 반려: step={}", currentStep.getStep());
        } else {
            throw new IllegalArgumentException("잘못된 상태값입니다: " + request.getStatus());
        }
        
        // 6. 저장
        approvalRequestRepository.save(approvalRequest);
        
        // 7. 결과 반환
        return ReturnApprovalResult.builder()
                .step(currentStep.getStep())
                .approverId(currentStep.getApproverId())
                .status(currentStep.getStatus().name().toLowerCase())
                .updatedAt(currentStep.getProcessedAt())
                .build();
    }
    
    /**
     * 승인 요청 상세 조회 (REST API: GET /process/{approverId})
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
