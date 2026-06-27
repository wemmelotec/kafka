package com.poc.notificationservice.application.service;

import com.poc.notificationservice.adapters.in.messaging.OrderCreatedEvent;
import com.poc.notificationservice.application.port.in.OrderCreatedHandler;
import com.poc.notificationservice.application.port.out.NotificationSender;
import com.poc.notificationservice.domain.Notification;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatedHandlerService implements OrderCreatedHandler {

    private final NotificationSender notificationSender;

    public OrderCreatedHandlerService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @Override
    public void handle(OrderCreatedEvent event) {
        Notification notification = Notification.from(event);
        notificationSender.send(notification);
    }
}
