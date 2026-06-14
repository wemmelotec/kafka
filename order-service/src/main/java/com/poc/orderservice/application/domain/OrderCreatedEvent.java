package com.poc.orderservice.application.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de domínio que representa a criação de um pedido, publicado pelo
 * {@code OrderEventPublisher} no tópico {@code orders.created}.
 */
public record OrderCreatedEvent(UUID orderId, String cpf, BigDecimal totalValue, Instant occurredAt) {

    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(order.id(), order.cpf(), order.totalValue(), order.createdAt());
    }
}
