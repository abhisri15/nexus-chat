package com.nexuschat.observer;

import com.nexuschat.client.ConnectedClient;
import com.nexuschat.message.Message;
import com.nexuschat.room.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observer implementation that logs all room events to console via SLF4J.
 *
 * Thread-safe: SLF4J/Logback handles concurrent writes internally.
 */
public class ConsoleRoomLogger implements RoomEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleRoomLogger.class);

    @Override
    public void onClientJoined(ConnectedClient client, Room room) {
        // TODO: Log "[JOIN] {username} joined #{roomName} (members: {count})"
    }

    @Override
    public void onClientLeft(ConnectedClient client, Room room) {
        // TODO: Log "[LEAVE] {username} left #{roomName} (members: {count})"
    }

    @Override
    public void onMessageBroadcast(Message message, Room room) {
        // TODO: Log "[MSG] #{roomName} {sender}: {content} | {queueStatus}"
    }

    @Override
    public void onRoomCreated(Room room) {
        // TODO: Log "[ROOM+] Created #{roomName}"
    }

    @Override
    public void onRoomDestroyed(Room room) {
        // TODO: Log "[ROOM-] Destroyed #{roomName} (total messages: {count})"
    }

    @Override
    public void onError(String source, Exception e) {
        // TODO: Log "[ERROR] {source}: {e.getMessage()}" at error level
    }
}
