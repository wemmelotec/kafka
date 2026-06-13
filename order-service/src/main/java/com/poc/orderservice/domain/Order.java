package com.poc.orderservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entidade de domínio que representa um pedido. Não possui anotações de
 * persistência, HTTP ou mensageria — depende apenas de {@code java.*}.
 */
public record Order(UUID id, String cpf, List<OrderItem> items, Instant createdAt) {

    public static Order create(String cpf, OrderItem item) {
        return new Order(UUID.randomUUID(), cpf, List.of(item), Instant.now());
    }

    public BigDecimal totalValue() {
        return items.stream()
                .map(OrderItem::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
