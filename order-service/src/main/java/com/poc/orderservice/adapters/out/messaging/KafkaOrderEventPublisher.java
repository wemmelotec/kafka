package com.poc.orderservice.adapters.out.messaging;

import com.poc.orderservice.application.port.out.OrderEventPublisher;
import com.poc.orderservice.domain.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter de saída real para o Kafka. Publica {@link OrderCreatedEvent} no
 * tópico configurado em {@code app.kafka.topic.order-created}, usando o
 * {@code orderId} como key da mensagem - garante que eventos do mesmo pedido
 * caiam sempre na mesma partição (ordem preservada).
 */
@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaOrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topic.order-created}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        String key = event.orderId().toString();
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Falha ao publicar OrderCreatedEvent orderId={} no topico {}", key, topic, ex);
            } else {
                log.info("OrderCreatedEvent orderId={} publicado em {}-{}@{}",
                        key,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
