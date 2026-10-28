package com.oeurn_nurak.marketplace.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.oeurn_nurak.marketplace.repository.UserRepository;

import java.util.Map;

/** Authenticates the notification socket with the same JWT used by the REST API. */
@Component
@RequiredArgsConstructor
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository users;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return reject(response);

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank() || !jwtService.isTokenValid(token)) {
            return reject(response);
        }

        try {
            String email = jwtService.extractUsername(token);
            var user = users.findByEmail(email)
                    .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                    .orElse(null);
            if (user == null) return reject(response);
            attributes.put("userId", user.getId());
            return true;
        } catch (RuntimeException ex) {
            return reject(response);
        }
    }

    private boolean reject(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
