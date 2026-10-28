package com.oeurn_nurak.marketplace.realtime;

import com.oeurn_nurak.marketplace.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;

/** Emits notification events only after the surrounding transaction commits. */
@Component
@RequiredArgsConstructor
public class NotificationRealtimePublisher {

    private final NotificationWebSocketHandler handler;

    public void created(Long userId, NotificationResponse notification, long unreadCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "notification.created");
        payload.put("notification", notification);
        payload.put("unreadCount", unreadCount);
        sendAfterCommit(userId, payload);
    }

    public void read(Long userId, Long notificationId, long unreadCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "notification.read");
        payload.put("id", notificationId);
        payload.put("unreadCount", unreadCount);
        sendAfterCommit(userId, payload);
    }

    public void readAll(Long userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "notification.read-all");
        payload.put("unreadCount", 0);
        sendAfterCommit(userId, payload);
    }

    private void sendAfterCommit(Long userId, Map<String, Object> payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    handler.send(userId, payload);
                }
            });
        } else {
            handler.send(userId, payload);
        }
    }
}
