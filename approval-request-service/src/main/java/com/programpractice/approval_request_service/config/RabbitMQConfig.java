package com.programpractice.approval_request_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RabbitMQConfig {
    
    // Exchange 이름
    public static final String APPROVAL_EXCHANGE = "approval.exchange";

    // Queue 이름
    public static final String APPROVAL_REQUEST_QUEUE = "approval.request.queue";
    public static final String APPROVAL_RESPONSE_QUEUE = "approval.response.queue";
    
    // Routing Key
    public static final String APPROVAL_REQUEST_ROUTING_KEY = "approval.request";
    public static final String APPROVAL_RESPONSE_ROUTING_KEY = "approval.response";
    
    /**
     * RabbitAdmin - 자동으로 Queue, Exchange, Binding 생성
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        log.info("RabbitAdmin 생성 완료");
        return admin;
    }
    
    /**
     * Exchange 생성
     */
    @Bean
    public TopicExchange approvalExchange() {
        TopicExchange exchange = new TopicExchange(APPROVAL_EXCHANGE, true, false);
        log.info("TopicExchange 생성: {}", APPROVAL_EXCHANGE);
        return exchange;
    }

    /**
     * 승인 요청 Queue
     */
    @Bean
    public Queue approvalRequestQueue() {
        Queue queue = new Queue(APPROVAL_REQUEST_QUEUE, true);
        log.info("Queue 생성: {}", APPROVAL_REQUEST_QUEUE);
        return queue;
    }

    /**
     * 승인 응답 Queue
     */
    @Bean
    public Queue approvalResponseQueue() {
        Queue queue = new Queue(APPROVAL_RESPONSE_QUEUE, true);
        log.info("Queue 생성: {}", APPROVAL_RESPONSE_QUEUE);
        return queue;
    }
    
    /**
     * 승인 요청 Binding
     */
    @Bean
    public Binding approvalRequestBinding(
            @Qualifier("approvalRequestQueue") Queue approvalRequestQueue, 
            TopicExchange approvalExchange) {
        
        Binding binding = BindingBuilder
                .bind(approvalRequestQueue)
                .to(approvalExchange)
                .with(APPROVAL_REQUEST_ROUTING_KEY);
        
        log.info("Binding 생성: {} -> {} with key {}", 
                APPROVAL_EXCHANGE, APPROVAL_REQUEST_QUEUE, APPROVAL_REQUEST_ROUTING_KEY);
        
        return binding;
    }
    
    /**
     * 승인 응답 Binding
     */
    @Bean
    public Binding approvalResponseBinding(
            @Qualifier("approvalResponseQueue") Queue approvalResponseQueue, 
            TopicExchange approvalExchange) {
        
        Binding binding = BindingBuilder
                .bind(approvalResponseQueue)
                .to(approvalExchange)
                .with(APPROVAL_RESPONSE_ROUTING_KEY);
        
        log.info("Binding 생성: {} -> {} with key {}", 
                APPROVAL_EXCHANGE, APPROVAL_RESPONSE_QUEUE, APPROVAL_RESPONSE_ROUTING_KEY);
        
        return binding;
    }
    
    /**
     * JSON 메시지 변환기
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        log.info("Jackson2JsonMessageConverter 생성 완료");
        return converter;
    }
    
    /**
     * RabbitTemplate 설정
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        
        // 메시지 발행 확인 콜백 설정
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("메시지 발행 성공: {}", correlationData);
            } else {
                log.error("메시지 발행 실패: {}, cause: {}", correlationData, cause);
            }
        });
        
        // 메시지 반환 콜백 설정 (라우팅 실패 시)
        template.setReturnsCallback(returned -> {
            log.error("메시지 라우팅 실패!");
            log.error("Exchange: {}", returned.getExchange());
            log.error("RoutingKey: {}", returned.getRoutingKey());
            log.error("ReplyCode: {}", returned.getReplyCode());
            log.error("ReplyText: {}", returned.getReplyText());
        });
        
        log.info("RabbitTemplate 생성 완료");
        return template;
    }
}