package com.poc.orderservice.adapters.out.messaging;

import com.poc.orderservice.application.port.out.OrderEventPublisher;
import com.poc.orderservice.domain.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub do adapter de saída para o Kafka. Implementa {@link OrderEventPublisher}
 * apenas registrando o evento em log; será substituído por um
 * {@code KafkaTemplate} real em uma etapa futura da POC, sem qualquer
 * alteração no caso de uso.
 */
@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    @Override
    public void publish(OrderCreatedEvent event) {
        log.info("[KAFKA-STUB] topic=order-created event={}", event);
    }
}
