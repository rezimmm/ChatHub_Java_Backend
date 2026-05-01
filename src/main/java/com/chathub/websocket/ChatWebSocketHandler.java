package com.chathub.websocket;

import com.chathub.model.Channel;
import com.chathub.model.User;
import com.chathub.repository.ChannelRepository;
import com.chathub.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;

    // userId -> WebSocketSession
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Verify user exists
        boolean exists = userRepository.findByCustomId(userId).isPresent();
        if (!exists) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        activeSessions.put(userId, session);

        try {
            // Mark user online
            userRepository.findByCustomId(userId)
                    .ifPresent(user -> {
                        user.setOnline(true);
                        user.setLastSeen(Instant.now().toString());
                        userRepository.save(user);
                    });
        } catch (Exception e) {
            log.error("Error updating online status for user {}: {}", userId, e.getMessage());
        }

        // Broadcast status on a separate thread — writing to the session during
        // afterConnectionEstablished can throw IllegalStateException (TEXT_PARTIAL_WRITING).
        String finalUserId = userId;
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            broadcastUserStatus(finalUserId, true, null);
        }, "ws-status-broadcast").start();

        log.info("WebSocket connected: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = extractUserId(session);
        if (userId == null) return;

        // Rate Limit Messages
        if (!rateLimitService.resolveMessageBucket(userId).tryConsume(1)) {
            log.warn("Rate limit exceeded for WebSocket user: {}", userId);
            // Optionally send an error message back to the user
            Map<String, Object> error = new HashMap<>();
            error.put("type", "error");
            error.put("message", "You are sending messages too fast. Slow down!");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            return;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");

            if ("pong".equals(type)) {
                return; // heartbeat response, ignore
            }

            if ("typing".equals(type)) {
                String channelId = (String) data.get("channel_id");
                Map<String, Object> typingMsg = new HashMap<>();
                typingMsg.put("type", "typing");
                typingMsg.put("user_id", userId);
                typingMsg.put("username", data.get("username"));
                typingMsg.put("channel_id", channelId);
                typingMsg.put("is_typing", data.get("is_typing"));
                broadcastToChannel(typingMsg, channelId);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message from {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId == null) return;

        activeSessions.remove(userId);

        try {
            // Mark user offline
            userRepository.findByCustomId(userId)
                    .ifPresent(user -> {
                        user.setOnline(false);
                        user.setLastSeen(Instant.now().toString());
                        userRepository.save(user);
                    });

            broadcastUserStatus(userId, false, null);
        } catch (Exception e) {
            log.error("Error updating status for disconnected user {}: {}", userId, e.getMessage());
        }
        
        log.info("WebSocket disconnected: userId={}, status={}", userId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Log the error but don't call afterConnectionClosed manually; 
        // the framework will invoke afterConnectionClosed automatically.
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    // ── Broadcast helpers ────────────────────────────────────────────────────

    public void sendToUser(Map<String, Object> message, String userId) {
        WebSocketSession session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            synchronized (session) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                } catch (IOException e) {
                    log.error("Failed to send message to user {}: {}", userId, e.getMessage());
                    activeSessions.remove(userId);
                }
            }
        }
    }

    public void broadcastToChannel(Map<String, Object> message, String channelId) {
        channelRepository.findByChannelId(channelId).ifPresent(channel -> {
            for (String memberId : channel.getMembers()) {
                sendToUser(message, memberId);
            }
        });
    }

    public void broadcastToAll(Map<String, Object> message) {
        for (String userId : activeSessions.keySet()) {
            sendToUser(message, userId);
        }
    }

    public void broadcastUserStatus(String userId, boolean isOnline, String status) {
        userRepository.findByCustomId(userId)
                .ifPresent(user -> {
                    Map<String, Object> statusMsg = new HashMap<>();
                    statusMsg.put("type", "user_status");
                    statusMsg.put("user_id", userId);
                    statusMsg.put("is_online", isOnline);
                    statusMsg.put("status", status != null ? status : user.getStatus());
                    statusMsg.put("last_seen", Instant.now().toString());
                    statusMsg.put("timestamp", Instant.now().toString());
                    broadcastToAll(statusMsg);
                });
    }

    public void sendPingToAll() {
        Map<String, Object> ping = Map.of("type", "ping");
        for (Map.Entry<String, WebSocketSession> entry : activeSessions.entrySet()) {
            try {
                if (entry.getValue().isOpen()) {
                    entry.getValue().sendMessage(new TextMessage(objectMapper.writeValueAsString(ping)));
                }
            } catch (IOException e) {
                activeSessions.remove(entry.getKey());
            }
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private String extractUserId(WebSocketSession session) {
        // URL is /api/ws/{userId}
        String path = session.getUri() != null ? session.getUri().getPath() : null;
        if (path == null) return null;
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
