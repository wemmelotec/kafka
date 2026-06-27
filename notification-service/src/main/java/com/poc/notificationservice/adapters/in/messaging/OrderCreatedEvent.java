package com.poc.notificationservice.adapters.in.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, String cpf, BigDecimal totalValue, Instant occurredAt) {
}
