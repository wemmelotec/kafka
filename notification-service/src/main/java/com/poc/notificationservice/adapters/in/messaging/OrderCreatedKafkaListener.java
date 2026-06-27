package com.poc.notificationservice.adapters.in.messaging;

import com.poc.notificationservice.application.port.in.OrderCreatedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedKafkaListener.class);

    private final OrderCreatedHandler handler;

    public OrderCreatedKafkaListener(OrderCreatedHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(OrderCreatedEvent event) {
        log.info("Evento recebido do Kafka: orderId={}", event.orderId());
        handler.handle(event);
    }
}
