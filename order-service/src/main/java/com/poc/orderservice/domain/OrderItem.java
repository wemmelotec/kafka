package com.poc.orderservice.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item de um pedido. Value object imutável, sem dependências de framework.
 */
public record OrderItem(String description, BigDecimal value) {

    public OrderItem {
        Objects.requireNonNull(description, "description não pode ser nulo");
        Objects.requireNonNull(value, "value não pode ser nulo");

        if (description.isBlank()) {
            throw new IllegalArgumentException("description não pode ser vazio");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("value deve ser maior que zero");
        }
    }
}
