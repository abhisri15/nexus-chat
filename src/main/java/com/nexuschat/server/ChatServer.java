package com.nexuschat.server;

import com.nexuschat.client.ClientHandler;
import com.nexuschat.client.ClientRegistry;
import com.nexuschat.client.ConnectedClient;
import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.room.RoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Core server class. Responsibilities:
 * 1. Open a ServerSocket and accept incoming connections
 * 2. Wrap each connection in a ConnectedClient
 * 3. Submit a ClientHandler (producer) to the thread pool
 * 4. Coordinate graceful shutdown
 */
public class ChatServer {

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private final ServerConfig config;
    private final RoomManager roomManager;
    private final ClientRegistry clientRegistry;
    private final RoomEventListener eventListener;
    private ExecutorService clientThreadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(ServerConfig config, RoomEventListener eventListener) {
        this.config = config;
        this.eventListener = eventListener;
        this.clientRegistry = new ClientRegistry();
        this.roomManager = new RoomManager(config, eventListener);
        this.running = false;
    }

    /**
     * Starts the server. This method BLOCKS on the accept loop.
     * Call from main thread; use shutdown hook to call stop().
     */
    public void start() {
        // TODO: 1. Create thread pool from config.getThreadPoolSize()
        // TODO: 2. Open ServerSocket on config.getPort()
        // TODO: 3. Set running = true, log startup
        // TODO: 4. Enter accept loop:
        //          - serverSocket.accept() → socket
        //          - Create ConnectedClient(socket)
        //          - Create ClientHandler(client, roomManager, clientRegistry, eventListener)
        //          - Submit handler to thread pool
        // TODO: 5. Catch SocketException when stop() closes the socket (normal shutdown)
    }

    /**
     * Gracefully shuts down the server.
     * Called from shutdown hook or programmatically.
     */
    public void stop() {
        // TODO: 1. Set running = false
        // TODO: 2. Broadcast shutdown message to all clients via clientRegistry
        // TODO: 3. Call roomManager.shutdownAllRooms()
        // TODO: 4. Close serverSocket (breaks accept loop)
        // TODO: 5. Shutdown thread pool (shutdownNow + awaitTermination)
        // TODO: 6. Disconnect all remaining clients
        // TODO: 7. Log shutdown complete
    }

    public boolean isRunning() {
        return running;
    }

    public ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }
}
