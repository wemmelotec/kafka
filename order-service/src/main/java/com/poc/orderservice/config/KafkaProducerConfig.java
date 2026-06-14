package com.poc.orderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Producer Kafka configurado explicitamente, em vez de depender apenas das
 * propriedades {@code spring.kafka.producer.*}.
 *
 * <p>Motivo: o construtor padrão de {@link JsonSerializer} (usado quando o
 * serializer é apenas referenciado por nome de classe via propriedades) não
 * registra o {@code JavaTimeModule}. Isso faz com que {@code Instant
 * occurredAt} de {@link com.poc.orderservice.domain.OrderCreatedEvent} seja
 * serializado como timestamp numérico (ex: {@code 1781369447.1906019}) em vez
 * de ISO-8601 - comportamento confirmado empiricamente ao validar esta etapa.
 * Por isso o {@link JsonSerializer} é construído aqui com um
 * {@link ObjectMapper} próprio, com {@code JavaTimeModule} registrado.</p>
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Map<String, Object> configProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        return new DefaultKafkaProducerFactory<>(configProps, null, () -> new JsonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
