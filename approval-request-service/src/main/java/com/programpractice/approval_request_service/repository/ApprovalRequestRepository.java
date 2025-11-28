package com.programpractice.approval_request_service.repository;

import com.programpractice.approval_request_service.document.ApprovalRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends MongoRepository<ApprovalRequest, String> {
    
    // requestId로 조회
    Optional<ApprovalRequest> findByRequestId(Integer requestId);
    
    // requestId 존재 여부 확인
    boolean existsByRequestId(Integer requestId);
}
