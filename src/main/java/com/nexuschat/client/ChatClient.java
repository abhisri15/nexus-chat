package com.nexuschat.client;

import com.nexuschat.room.Room;

/**
 * Abstraction for a connected chat client (Dependency Inversion).
 *
 * Both TCP clients (ConnectedClient) and WebSocket clients (WebSocketChatClient)
 * implement this interface, allowing Room, MessageBroadcaster, and ClientRegistry
 * to work with any transport without knowing the details.
 */
public interface ChatClient {

    String getClientId();

    String getUsername();

    void setUsername(String username);

    Room getCurrentRoom();

    void setCurrentRoom(Room room);

    boolean isConnected();

    void sendMessage(String rawMessage);

    void disconnect();
}
