package com.nexuschat;

import com.nexuschat.http.HttpStaticServer;
import com.nexuschat.observer.ConsoleRoomLogger;
import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.server.ChatServer;
import com.nexuschat.server.ServerConfig;
import com.nexuschat.websocket.WebSocketChatServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the NexusChat server.
 *
 * Starts three subsystems that share RoomManager and ClientRegistry:
 *   1. HTTP static server  (port 8080) — serves the web UI
 *   2. WebSocket server    (port 9091) — real-time browser messaging
 *   3. TCP server          (port 9090) — CLI client connections (blocks)
 */
public class NexusChatServer {

    private static final Logger logger = LoggerFactory.getLogger(NexusChatServer.class);

    private static final int HTTP_PORT = 8080;
    private static final int WS_PORT = 9091;

    public static void main(String[] args) {
        ServerConfig config = new ServerConfig();
        RoomEventListener eventListener = new ConsoleRoomLogger();

        ChatServer tcpServer = new ChatServer(config, eventListener);

        WebSocketChatServer wsServer = new WebSocketChatServer(
                WS_PORT, tcpServer.getRoomManager(),
                tcpServer.getClientRegistry(), eventListener);

        HttpStaticServer httpServer = new HttpStaticServer(
                HTTP_PORT, tcpServer.getRoomManager(), tcpServer.getClientRegistry());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tcpServer.stop();
            try { wsServer.stop(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            httpServer.stop();
        }, "shutdown-hook"));

        logger.info("Starting NexusChat...");

        try {
            httpServer.start();
            logger.info("Web UI → http://localhost:{}", HTTP_PORT);
        } catch (Exception e) {
            logger.error("Failed to start HTTP server: {}", e.getMessage());
        }

        wsServer.start();
        logger.info("WebSocket server → ws://localhost:{}", WS_PORT);

        tcpServer.start();
    }
}
