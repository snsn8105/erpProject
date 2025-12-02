package com.programpractice.approval_processing_service.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

import com.programpractice.approval_processing_service.model.ApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalStatus;

// Map 기반 In-Memory 방식 승인 요청 저장소
@Repository
@Slf4j
public class InMemoryApprovalRepository {
    
    // Thread-Safe한 Map 사용
    private final Map<Integer, ApprovalRequest> requestIdMap = new ConcurrentHashMap<>();
    private final Map<Long, ApprovalRequest> idMap = new ConcurrentHashMap<>(); // requesterId 매핑용

    public ApprovalRequest save(ApprovalRequest request) {

        requestIdMap.put(request.getRequestId(), request);
        idMap.put(request.getRequesterId(), request);
    
        log.debug("승인 요청 저장: id={}, requestId={}", request.getId(), request.getRequestId());
        return request;
    }

    // requestId로 조회
    public Optional<ApprovalRequest> findByRequestId(Integer requestId) {
        ApprovalRequest request = requestIdMap.get(requestId);
        return Optional.ofNullable(request);
    }
    
    // ID로 조회
    public Optional<ApprovalRequest> findById(Long requesterId) {
        ApprovalRequest request = idMap.get(requesterId);
        return Optional.ofNullable(request);
    }
    
    // approverId로 대기 중인 승인 건 조회
    public List<ApprovalRequest> findPendingApprovalsByApproverId(Long approverId) {
        return requestIdMap.values().stream()
                .filter(request -> request.getSteps().stream()
                        .anyMatch(step -> 
                                step.getApproverId().equals(approverId) && 
                                step.getStatus() == ApprovalStatus.PENDING))
                .collect(Collectors.toList());
    }
    
    // 전체 조회
    
    public List<ApprovalRequest> findAll() {
        return new ArrayList<>(requestIdMap.values());
    }
    
    // 존재 여부 확인
    
    public boolean existsByRequestId(Integer requestId) {
        return requestIdMap.containsKey(requestId);
    }
    
    // 삭제
    
    public void delete(ApprovalRequest request) {
        requestIdMap.remove(request.getRequestId());
        idMap.remove(request.getRequesterId());
        log.debug("승인 요청 삭제: id={}, requestId={}", request.getId(), request.getRequestId());
    }
    
    // 전체 삭제 (테스트용)
    
    public void deleteAll() {
        requestIdMap.clear();
        idMap.clear();
        log.info("모든 승인 요청 삭제");
    }
    
    // 개수 조회
    
    public long count() {
        return requestIdMap.size();
    }    
}
