package com.programpractice.employee_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.programpractice.employee_service.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    List<Employee> findByDepartmentAndPosition(String department, String position);
    
    /**
     * 부서로 직원 검색
     */
    List<Employee> findByDepartment(String department);
    
    /**
     * 직책으로 직원 검색
     */
    List<Employee> findByPosition(String position);
}
