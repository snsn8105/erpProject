package com.programpractice.approval_request_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Exchange 이름
    public static final String APPROVAL_EXCHANGE = "approval.exchange";

    // Queue 이름
    public static final String APPROVAL_REQUEST_QUEUE = "approval.request.queue";
    public static final String APPROVAL_RESPONSE_QUEUE = "approval.response.queue";
    
    // Routing Key
    public static final String APPROVAL_REQUEST_ROUTING_KEY = "approval.request";
    public static final String APPROVAL_RESPONSE_ROUTING_KEY = "approval.response";
    
    // Exchange 생성
    @Bean
    public TopicExchange approvalExchange() {
        return new TopicExchange(APPROVAL_EXCHANGE);
    }

    // 승인 요청 Queue
    @Bean
    public Queue approvalRequestQueue() {
        return new Queue(APPROVAL_REQUEST_QUEUE, true);
    }

    // 승인 응답 Queue
    @Bean
    public Queue approvalResponseQueue() {
        return new Queue(APPROVAL_RESPONSE_QUEUE, true);
    }
    
    // 승인 요청 Binding
    @Bean
    public Binding approvalRequestBinding(@Qualifier("approvalRequestQueue") Queue approvalRequestQueue, TopicExchange approvalExchange) {
        return BindingBuilder
                .bind(approvalRequestQueue)
                .to(approvalExchange)
                .with(APPROVAL_REQUEST_ROUTING_KEY);
    }
    
    // 승인 응답 Binding
    @Bean
    public Binding approvalResponseBinding(@Qualifier("approvalResponseQueue") Queue approvalResponseQueue, TopicExchange approvalExchange) {
        return BindingBuilder
                .bind(approvalResponseQueue)
                .to(approvalExchange)
                .with(APPROVAL_RESPONSE_ROUTING_KEY);
    }
    
    // JSON 메시지 변환기
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // RabbitTemplate 설정
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
