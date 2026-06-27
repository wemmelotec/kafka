package com.poc.notificationservice.application.port.out;

import com.poc.notificationservice.domain.Notification;

public interface NotificationSender {

    void send(Notification notification);
}
