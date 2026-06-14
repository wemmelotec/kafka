package com.poc.orderservice.application.service;

import com.poc.orderservice.application.domain.Order;
import com.poc.orderservice.application.domain.OrderCreatedEvent;
import com.poc.orderservice.application.domain.OrderItem;
import com.poc.orderservice.application.port.in.CreateOrderCommand;
import com.poc.orderservice.application.port.in.CreateOrderUseCase;
import com.poc.orderservice.application.port.out.OrderEventPublisher;
import com.poc.orderservice.util.UseCase;

/**
 * Caso de uso: cria o pedido a partir do comando recebido e publica o
 * {@link OrderCreatedEvent} correspondente através do {@link OrderEventPublisher}.
 */
@UseCase
public class CreateOrderService implements CreateOrderUseCase {

    private static final String ITEM_DESCRIPTION = "Solicitacao de pedido baseada em renda informada";

    private final OrderEventPublisher orderEventPublisher;

    public CreateOrderService(OrderEventPublisher orderEventPublisher) {
        this.orderEventPublisher = orderEventPublisher;
    }

    @Override
    public Order createOrder(CreateOrderCommand command) {
        OrderItem item = new OrderItem(ITEM_DESCRIPTION, command.salario());
        Order order = Order.create(command.cpf(), item);

        orderEventPublisher.publish(OrderCreatedEvent.from(order));

        return order;
    }
}
