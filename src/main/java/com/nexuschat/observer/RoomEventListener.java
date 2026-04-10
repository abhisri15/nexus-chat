package com.nexuschat.observer;

import com.nexuschat.client.ChatClient;
import com.nexuschat.message.Message;
import com.nexuschat.room.Room;

/**
 * Observer interface for room lifecycle and message events.
 *
 * Decouples monitoring/logging from business logic.
 * Implementations can log to console, write metrics, send alerts, etc.
 *
 * Called from multiple threads (ClientHandler, MessageBroadcaster, RoomManager)
 * — implementations must be thread-safe.
 */
public interface RoomEventListener {

    void onClientJoined(ChatClient client, Room room);

    void onClientLeft(ChatClient client, Room room);

    void onMessageBroadcast(Message message, Room room);

    void onRoomCreated(Room room);

    void onRoomDestroyed(Room room);

    void onError(String source, Exception e);
}
