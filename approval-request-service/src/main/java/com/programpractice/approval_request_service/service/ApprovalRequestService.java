package com.programpractice.approval_request_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.programpractice.approval_request_service.client.EmployeeServiceClient;
import com.programpractice.approval_request_service.document.ApprovalRequest;
import com.programpractice.approval_request_service.dto.ApprovalCreateRequest;
import com.programpractice.approval_request_service.dto.ApprovalCreateResponse;
import com.programpractice.approval_request_service.dto.ApprovalRequestMessage;
import com.programpractice.approval_request_service.dto.ApprovalResponse;
import com.programpractice.approval_request_service.dto.StepRequest;
import com.programpractice.approval_request_service.exception.ApprovalRequestNotFoundException;
import com.programpractice.approval_request_service.exception.InvalidApprovalStepsException;
import com.programpractice.approval_request_service.repository.ApprovalRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalRequestService {
    
    private final ApprovalRequestRepository approvalRequestRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final EmployeeServiceClient employeeServiceClient;
    private final ApprovalMessagePublisher messagePublisher;  // 추가
    
    private static final String APPROVAL_REQUEST_SEQ = "approval_request_id";
    
    /**
     * 결재 요청 생성
     */
    public ApprovalCreateResponse createApprovalRequest(ApprovalCreateRequest request) {
        log.info("결재 요청 생성: requesterId={}, title={}", 
                request.getRequesterId(), request.getTitle());
        
        // 1. 요청자 존재 여부 확인
        validateEmployee(request.getRequesterId(), "요청자");
        
        // 2. 결재자들 존재 여부 확인
        for (StepRequest step : request.getSteps()) {
            validateEmployee(step.getApproverId(), "결재자");
        }
        
        // 3. 단계 검증
        validateSteps(request.getSteps());
        
        // 4. requestId 생성
        int requestId = sequenceGeneratorService.generateSequence(APPROVAL_REQUEST_SEQ);
        
        // 5. steps에 status "pending" 추가
        List<com.programpractice.approval_request_service.document.Step> steps = request.getSteps().stream()
                .map(s -> com.programpractice.approval_request_service.document.Step.builder()
                        .step(s.getStep())
                        .approverId(s.getApproverId())
                        .status("pending")
                        .build())
                .collect(Collectors.toList());
        
        // 6. ApprovalRequest 생성 및 저장
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestId(requestId)
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .content(request.getContent())
                .steps(steps)
                .finalStatus("in_progress")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
        
        log.info("결재 요청 생성 완료: requestId={}, mongoId={}", requestId, saved.getId());
        
        // 7. RabbitMQ로 Processing Service에 메시지 발행
        publishApprovalRequestMessage(saved);
        
        return new ApprovalCreateResponse(requestId);
    }
    
    /**
     * RabbitMQ로 승인 요청 메시지 발행
     */
    private void publishApprovalRequestMessage(ApprovalRequest approvalRequest) {
        try {
            log.info("=== 승인 요청 메시지 발행 시작 ===");

            List<ApprovalRequestMessage.ApprovalStepDto> stepDtos = approvalRequest.getSteps().stream()
                    .map(step -> ApprovalRequestMessage.ApprovalStepDto.builder()
                            .step(step.getStep())
                            .approverId(step.getApproverId().longValue())
                            .build())
                    .collect(Collectors.toList());

            ApprovalRequestMessage message = ApprovalRequestMessage.builder()
                    .id(approvalRequest.getId())
                    .requestId(approvalRequest.getRequestId())
                    .requesterId(approvalRequest.getRequesterId().longValue())
                    .requesterName("Requester_" + approvalRequest.getRequesterId())
                    .title(approvalRequest.getTitle())
                    .content(approvalRequest.getContent())
                    .requestedAt(approvalRequest.getCreatedAt())
                    .steps(stepDtos)
                    .build();
            
            messagePublisher.publishApprovalRequest(message);
            log.info("승인 요청 메시지 발행 완료: requestId={}", approvalRequest.getRequestId());
            
        } catch (Exception e) {
            log.error("승인 요청 메시지 발행 실패: requestId={}", approvalRequest.getRequestId(), e);
        }
    }
    
    /**
     * 결재 요청 목록 조회
     */
    public List<ApprovalResponse> getAllApprovalRequests() {
        log.info("결재 요청 목록 조회");
        
        List<ApprovalRequest> requests = approvalRequestRepository.findAll();
        
        return requests.stream()
                .map(ApprovalResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 결재 요청 상세 조회
     */
    public ApprovalResponse getApprovalRequest(Integer requestId) {
        log.info("결재 요청 상세 조회: requestId={}", requestId);
        
        ApprovalRequest request = approvalRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ApprovalRequestNotFoundException(
                        "결재 요청을 찾을 수 없습니다: requestId=" + requestId));
        
        return ApprovalResponse.from(request);
    }
    
    /**
     * 직원 존재 여부 검증
     */
    private void validateEmployee(Integer employeeId, String role) {
        if (!employeeServiceClient.existsEmployee(employeeId)) {
            throw new InvalidApprovalStepsException(
                    role + "가 존재하지 않습니다: employeeId=" + employeeId);
        }
    }
    
    /**
     * 결재 단계 검증
     */
    private void validateSteps(List<StepRequest> steps) {
        if (steps.isEmpty()) {
            throw new InvalidApprovalStepsException("결재 단계가 비어있습니다");
        }
        
        // 1부터 시작하는지 확인
        if (steps.get(0).getStep() != 1) {
            throw new InvalidApprovalStepsException("결재 단계는 1부터 시작해야 합니다");
        }
        
        // 순차적으로 증가하는지 확인
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getStep() != i + 1) {
                throw new InvalidApprovalStepsException(
                        "결재 단계는 1부터 순차적으로 증가해야 합니다");
            }
        }
    }
}
