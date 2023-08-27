package com.dansplugins.detectionsystem.notifications;

import java.util.UUID;

public interface NotificationService {
    public void sendNotification(UUID recipient, String title, String body);
}
