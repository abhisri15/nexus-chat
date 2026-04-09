package com.nexuschat.server;

/**
 * Immutable configuration for the NexusChat server.
 *
 * All values have sensible defaults. Use the parameterized constructor
 * to override for testing or deployment tuning.
 */
public class ServerConfig {

    // --- Defaults ---
    public static final int DEFAULT_PORT = 9090;
    public static final int DEFAULT_MAX_CLIENTS = 200;
    public static final int DEFAULT_ROOM_QUEUE_CAPACITY = 50;
    public static final int DEFAULT_THREAD_POOL_SIZE = 100;
    public static final long DEFAULT_CLIENT_TIMEOUT_MS = 30_000;

    private final int port;
    private final int maxClients;
    private final int roomQueueCapacity;
    private final int threadPoolSize;
    private final long clientTimeoutMs;

    public ServerConfig() {
        this(DEFAULT_PORT, DEFAULT_MAX_CLIENTS, DEFAULT_ROOM_QUEUE_CAPACITY,
             DEFAULT_THREAD_POOL_SIZE, DEFAULT_CLIENT_TIMEOUT_MS);
    }

    public ServerConfig(int port, int maxClients, int roomQueueCapacity,
                        int threadPoolSize, long clientTimeoutMs) {
        this.port = port;
        this.maxClients = maxClients;
        this.roomQueueCapacity = roomQueueCapacity;
        this.threadPoolSize = threadPoolSize;
        this.clientTimeoutMs = clientTimeoutMs;
    }

    // --- Getters ---

    public int getPort() {
        return port;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public int getRoomQueueCapacity() {
        return roomQueueCapacity;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public long getClientTimeoutMs() {
        return clientTimeoutMs;
    }
}
