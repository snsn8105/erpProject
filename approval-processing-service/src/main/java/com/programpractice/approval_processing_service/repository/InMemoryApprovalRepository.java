package com.programpractice.approval_processing_service.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

import com.programpractice.approval_processing_service.model.ApprovalRequest;
import com.programpractice.approval_processing_service.model.ApprovalStatus;

// Map 기반 In-Memory 방식 승인 요청 저장소
@Repository
@Slf4j
public class InMemoryApprovalRepository {
    
    // Thread-Safe한 Map, Long 사용
    private final Map<String, ApprovalRequest> requestIdMap = new ConcurrentHashMap<>();
    private final Map<Long, ApprovalRequest> idMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final AtomicLong stepIdGenerator = new AtomicLong(1);

    public ApprovalRequest save(ApprovalRequest request) {
        if (request.getId() == null) {
            // 신규 저장
            Long newId = idGenerator.getAndIncrement();
            request.setId(newId);

            //Step ID 생성
            request.getSteps().forEach(step -> {
                if (step.getId() == null) {
                    step.setId(stepIdGenerator.getAndIncrement());
                }
            });
        }

        requestIdMap.put(request.getRequestId(), request);
        idMap.put(request.getId(), request);
    
        log.debug("승인 요청 저장: id={}, requestId={}", request.getId(), request.getRequestId());
        return request;
    }

    // requestId로 조회
    public Optional<ApprovalRequest> findByRequestId(String requestId) {
        ApprovalRequest request = requestIdMap.get(requestId);
        return Optional.ofNullable(request);
    }
    
    // ID로 조회
    public Optional<ApprovalRequest> findById(Long id) {
        ApprovalRequest request = idMap.get(id);
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
    
    public boolean existsByRequestId(String requestId) {
        return requestIdMap.containsKey(requestId);
    }
    
    // 삭제
    
    public void delete(ApprovalRequest request) {
        requestIdMap.remove(request.getRequestId());
        idMap.remove(request.getId());
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
