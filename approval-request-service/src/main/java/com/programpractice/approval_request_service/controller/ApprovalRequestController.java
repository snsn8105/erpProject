package com.programpractice.approval_request_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.programpractice.approval_request_service.dto.ApprovalCreateRequest;
import com.programpractice.approval_request_service.dto.ApprovalCreateResponse;
import com.programpractice.approval_request_service.dto.ApprovalResponse;
import com.programpractice.approval_request_service.service.ApprovalRequestService;

import java.util.List;

@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalRequestController {
    
    private final ApprovalRequestService approvalRequestService;
    
    /**
     * POST /approvals
     * 결재 요청 생성
     */
    @PostMapping
    public ResponseEntity<ApprovalCreateResponse> createApproval(
            @Valid @RequestBody ApprovalCreateRequest request) {
        
        log.info("POST /approvals 호출");
        ApprovalCreateResponse response = approvalRequestService.createApprovalRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /approvals
     * 결재 요청 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ApprovalResponse>> getAllApprovals() {
        log.info("GET /approvals 호출");
        List<ApprovalResponse> approvals = approvalRequestService.getAllApprovalRequests();
        return ResponseEntity.ok(approvals);
    }
    
    /**
     * GET /approvals/{requestId}
     * 결재 요청 상세 조회
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ApprovalResponse> getApproval(@PathVariable Integer requestId) {
        log.info("GET /approvals/{} 호출", requestId);
        ApprovalResponse approval = approvalRequestService.getApprovalRequest(requestId);
        return ResponseEntity.ok(approval);
    }
}
