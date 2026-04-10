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
        clientThreadPool = Executors.newFixedThreadPool(config.getThreadPoolSize());

        try {
            serverSocket = new ServerSocket(config.getPort());
            running = true;
            logger.info("NexusChat server started on port {}", config.getPort());

            while (running) {
                Socket socket = serverSocket.accept();
                ConnectedClient client = new ConnectedClient(socket);
                ClientHandler handler = new ClientHandler(client, roomManager, clientRegistry, eventListener);
                clientThreadPool.submit(handler);
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Server error: {}", e.getMessage());
            }
        }
    }

    /**
     * Gracefully shuts down the server.
     * Called from shutdown hook or programmatically.
     */
    public void stop() {
        if (!running) return;
        running = false;
        logger.info("Shutting down NexusChat...");

        for (ConnectedClient client : clientRegistry.getAllClients()) {
            client.sendMessage("[SERVER] Server shutting down...");
        }

        roomManager.shutdownAllRooms();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }

        if (clientThreadPool != null) {
            clientThreadPool.shutdownNow();
            try {
                clientThreadPool.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (ConnectedClient client : clientRegistry.getAllClients()) {
            client.disconnect();
        }

        logger.info("NexusChat shut down.");
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
