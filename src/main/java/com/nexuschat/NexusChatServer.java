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
        // TODO: 1. Create ServerConfig (use defaults or parse args)
        // TODO: 2. Create RoomEventListener (ConsoleRoomLogger)
        // TODO: 3. Create ChatServer with config and listener
        // TODO: 4. Register shutdown hook → server.stop()
        // TODO: 5. Call server.start() (blocking — runs accept loop)
    }
}
