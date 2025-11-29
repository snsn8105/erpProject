package com.programpractice.approval_processing_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.programpractice.approval_processing_service.entity.ApprovalRequest;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    
    /**
     * requestId로 조회
     */
    Optional<ApprovalRequest> findByRequestId(String requestId);
    
    /**
     * requestId와 steps를 함께 조회 (N+1 방지)
     */
    @Query("SELECT a FROM ApprovalRequest a LEFT JOIN FETCH a.steps WHERE a.requestId = :requestId")
    Optional<ApprovalRequest> findByRequestIdWithSteps(@Param("requestId") String requestId);
    
    /**
     * approverId로 대기 중인 승인 건 조회
     */
    @Query("SELECT DISTINCT a FROM ApprovalRequest a " +
           "JOIN FETCH a.steps s " +
           "WHERE s.approverId = :approverId AND s.status = 'PENDING'")
    java.util.List<ApprovalRequest> findPendingApprovalsByApproverId(@Param("approverId") Long approverId);
}
