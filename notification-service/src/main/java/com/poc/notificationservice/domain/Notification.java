package com.poc.notificationservice.domain;

import com.poc.notificationservice.adapters.in.messaging.OrderCreatedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Notification(UUID orderId, String cpf, BigDecimal totalValue, Instant occurredAt) {

    public static Notification from(OrderCreatedEvent event) {
        return new Notification(event.orderId(), event.cpf(), event.totalValue(), event.occurredAt());
    }
}
