package com.programpractice.employee_service.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정
 * @CreatedDate를 사용하기 위해 필요
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
