package com.poc.notificationservice.application.port.in;

import com.poc.notificationservice.adapters.in.messaging.OrderCreatedEvent;

public interface OrderCreatedHandler {

    void handle(OrderCreatedEvent event);
}
