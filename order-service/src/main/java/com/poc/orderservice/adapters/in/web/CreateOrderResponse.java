package com.poc.orderservice.adapters.in.web;

import java.util.UUID;

/**
 * DTO de saída do endpoint {@code POST /api/orders}. Mensagem de confirmação
 * simples — não expõe a entidade de domínio {@code Order}.
 */
public record CreateOrderResponse(UUID orderId, String status, String message) {

    public static CreateOrderResponse received(UUID orderId) {
        return new CreateOrderResponse(orderId, "RECEIVED", "Pedido recebido e processado com sucesso.");
    }
}
