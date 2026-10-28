package com.oeurn_nurak.marketplace.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps one live socket set per user and pushes notification events to them. */
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        send(session, Map.of("event", "connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("ping".equals(message.getPayload())) {
            send(session, Map.of("event", "pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        if (userId == null) return;
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return;
        userSessions.remove(session);
        if (userSessions.isEmpty()) sessions.remove(userId, userSessions);
    }

    public void send(Long userId, Object event) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return;
        for (WebSocketSession session : userSessions) send(session, event);
    }

    private void send(WebSocketSession session, Object event) {
        if (!session.isOpen()) return;
        try {
            String payload = objectMapper.writeValueAsString(event);
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (Exception ex) {
            log.debug("Dropping notification socket for session {}", session.getId(), ex);
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        }
    }

    private Long userId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            // The transport is already gone.
        }
    }
}
