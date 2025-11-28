package com.programpractice.approval_request_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * Basic Authentication 사용
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API이므로)
            .csrf(csrf -> csrf.disable())
            
            // 세션 정책: STATELESS (REST API는 stateless)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 인증 규칙
            .authorizeHttpRequests(auth -> auth
                // 모든 엔드포인트는 인증 필요
                // .anyRequest().authenticated()
                
                // [변경] 모든 요청 허용 (테스트용)
                .anyRequest().permitAll()
            )
            
            // HTTP Basic Authentication 사용
            .httpBasic(basic -> {});
        
        return http.build();
    }
    
    /**
     * 사용자 정보 설정 (In-Memory)
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build();
        
        UserDetails requester = User.builder()
                .username("requester")
                .password(passwordEncoder().encode("req123"))
                .roles("REQUESTER")
                .build();
        
        UserDetails service = User.builder()
                .username("service")
                .password(passwordEncoder().encode("service123"))
                .roles("SERVICE")
                .build();
        
        return new InMemoryUserDetailsManager(admin, requester, service);
    }
    
    /**
     * 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
