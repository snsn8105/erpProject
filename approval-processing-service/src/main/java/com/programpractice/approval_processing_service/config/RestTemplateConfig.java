package com.programpractice.approval_processing_service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 설정
 * Employee Service 호출용
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Employee Service 인증 정보 (개발 환경은 인증 없음)
                // .basicAuthentication("test", "test")
                
                // Timeout 설정
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                
                // 에러 핸들러 (404는 예외로 처리하지 않음)
                .build();
    }
}