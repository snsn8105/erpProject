package com.programpractice.approval_processing_service.repository;

import java.util.List;
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
     * 주의: JOIN FETCH 대상(a.steps)에 별칭을 주고 WHERE 절에서 필터링하면 
     * 해당 컬렉션 데이터가 메모리상에서 잘려나가는(누락되는) 문제가 발생함.
     * 따라서 서브쿼리나 JOIN으로 대상 ID를 찾고, 데이터는 온전하게 다 가져와야 함.
     */
    @Query("SELECT DISTINCT a FROM ApprovalRequest a " +
        "JOIN FETCH a.steps " + 
        "WHERE a.id IN (" +
        "  SELECT s.approvalRequest.id " +
        "  FROM ApprovalStep s " +
        "  WHERE s.approverId = :approverId AND s.status = 'PENDING'" +
        ")")
    List<ApprovalRequest> findPendingApprovalsByApproverId(@Param("approverId") Long approverId);
}
