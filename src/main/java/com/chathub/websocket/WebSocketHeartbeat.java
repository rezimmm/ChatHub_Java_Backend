package com.chathub.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHeartbeat {

    private final ChatWebSocketHandler wsHandler;

    /** Send a ping to all connected clients every 30 seconds */
    @Scheduled(fixedDelay = 30_000)
    public void ping() {
        wsHandler.sendPingToAll();
    }
}
