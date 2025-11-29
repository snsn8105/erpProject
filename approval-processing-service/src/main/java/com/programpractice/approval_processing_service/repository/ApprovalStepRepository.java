package com.programpractice.approval_processing_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.programpractice.approval_processing_service.entity.ApprovalStep;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    // JpaRepository의 기본 로직만 포함
}
