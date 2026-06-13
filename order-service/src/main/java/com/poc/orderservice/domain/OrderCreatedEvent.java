package com.poc.orderservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de domínio que representa a criação de um pedido. Esta etapa não
 * publica o evento em nenhum broker real — a representação existe para que o
 * port de saída ({@code OrderEventPublisher}) tenha um contrato estável desde já.
 */
public record OrderCreatedEvent(UUID orderId, String cpf, BigDecimal totalValue, Instant occurredAt) {

    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(order.id(), order.cpf(), order.totalValue(), order.createdAt());
    }
}
