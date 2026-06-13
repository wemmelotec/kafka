package com.poc.orderservice.application.port.in;

import com.poc.orderservice.domain.Order;

/**
 * Input port: contrato do caso de uso de criação de pedido.
 * Implementado por {@code CreateOrderService} e consumido pelo adapter web.
 */
public interface CreateOrderUseCase {

    Order createOrder(CreateOrderCommand command);
}
