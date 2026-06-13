package com.poc.orderservice.application.port.out;

import com.poc.orderservice.domain.OrderCreatedEvent;

/**
 * Output port: contrato para publicação de eventos de domínio relacionados
 * a pedidos. A camada de aplicação depende apenas desta interface; quem a
 * implementa (ex: Kafka, fila, log) é decisão do adapter de saída.
 */
public interface OrderEventPublisher {

    void publish(OrderCreatedEvent event);
}
