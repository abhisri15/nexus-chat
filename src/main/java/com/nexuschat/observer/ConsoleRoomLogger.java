package com.nexuschat.observer;

import com.nexuschat.client.ChatClient;
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
    public void onClientJoined(ChatClient client, Room room) {
        logger.info("[JOIN] {} joined #{} (members: {})", client.getUsername(), room.getName(), room.getMemberCount());
    }

    @Override
    public void onClientLeft(ChatClient client, Room room) {
        logger.info("[LEAVE] {} left #{} (members: {})", client.getUsername(), room.getName(), room.getMemberCount());
    }

    @Override
    public void onMessageBroadcast(Message message, Room room) {
        logger.info("[MSG] #{} {}: {} | {}", room.getName(), message.getSender(), message.getContent(), room.getQueueStatus());
    }

    @Override
    public void onRoomCreated(Room room) {
        logger.info("[ROOM+] Created #{}", room.getName());
    }

    @Override
    public void onRoomDestroyed(Room room) {
        logger.info("[ROOM-] Destroyed #{} (total messages: {})", room.getName(), room.getMessageCount());
    }

    @Override
    public void onError(String source, Exception e) {
        logger.error("[ERROR] {}: {}", source, e.getMessage());
    }
}
