package com.poc.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Define o tópico Kafka publicado pelo order-service. O broker está
 * configurado com {@code auto.create.topics.enable=false}; este bean
 * {@code NewTopic} é detectado pelo {@code KafkaAdmin} (autoconfigurado pelo
 * Spring Boot a partir de {@code spring.kafka.bootstrap-servers}) e usado
 * para criar o tópico na inicialização da aplicação, caso ainda não exista.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.order-created}")
    private String orderCreatedTopic;

    @Bean
    public NewTopic ordersCreatedTopic() {
        return TopicBuilder.name(orderCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
