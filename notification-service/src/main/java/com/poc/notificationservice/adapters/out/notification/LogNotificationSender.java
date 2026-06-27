package com.poc.notificationservice.adapters.out.notification;

import com.poc.notificationservice.application.port.out.NotificationSender;
import com.poc.notificationservice.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("[NOTIFICATION] Pedido {} recebido - cpf={}, totalValue={}, notificacao enviada",
                notification.orderId(), notification.cpf(), notification.totalValue());
    }
}
