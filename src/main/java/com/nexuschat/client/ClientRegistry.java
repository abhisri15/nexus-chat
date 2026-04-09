package com.nexuschat.client;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of all connected clients.
 *
 * Uses ConcurrentHashMap for lock-free reads and segment-level writes.
 * Enforces unique usernames across the server.
 *
 * Accessed from: ClientHandler threads (register/unregister),
 *                ChatServer (broadcast shutdown), any thread (lookups)
 */
public class ClientRegistry {

    private final ConcurrentHashMap<String, ConnectedClient> clientsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConnectedClient> clientsByUsername = new ConcurrentHashMap<>();

    /**
     * Register a client after username is set.
     * Returns false if username is already taken.
     */
    public boolean register(ConnectedClient client) {
        // TODO: putIfAbsent on clientsByUsername (returns null if success)
        //       put on clientsById
        //       return success/failure
        return false;
    }

    /**
     * Unregister a client (on disconnect or quit).
     */
    public void unregister(String clientId) {
        // TODO: Remove from clientsById
        //       Remove from clientsByUsername (using the client's username)
    }

    public ConnectedClient getByUsername(String username) {
        return clientsByUsername.get(username);
    }

    public ConnectedClient getByClientId(String clientId) {
        return clientsById.get(clientId);
    }

    public Collection<ConnectedClient> getAllClients() {
        return clientsById.values();
    }

    public boolean isUsernameTaken(String username) {
        return clientsByUsername.containsKey(username);
    }

    public int getOnlineCount() {
        return clientsById.size();
    }
}
