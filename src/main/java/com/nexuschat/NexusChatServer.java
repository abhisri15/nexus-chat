package com.nexuschat;

import com.nexuschat.observer.ConsoleRoomLogger;
import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.server.ChatServer;
import com.nexuschat.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the NexusChat server.
 *
 * Wires up config, observer, and server — then starts accepting connections.
 * Registers a JVM shutdown hook for graceful teardown.
 */
public class NexusChatServer {

    private static final Logger logger = LoggerFactory.getLogger(NexusChatServer.class);

    public static void main(String[] args) {
        ServerConfig config = new ServerConfig();
        RoomEventListener eventListener = new ConsoleRoomLogger();
        ChatServer server = new ChatServer(config, eventListener);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown-hook"));

        logger.info("Starting NexusChat...");
        server.start();
    }
}
