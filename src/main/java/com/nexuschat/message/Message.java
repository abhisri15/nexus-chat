package com.nexuschat.message;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable message object that flows through the system.
 *
 * Created by ClientHandler (producer), enqueued into room's BoundedMessageQueue,
 * dequeued by MessageBroadcaster (consumer), delivered to room members.
 *
 * Immutability is critical — the same Message instance is read by multiple
 * threads (broadcaster + observer) without synchronization.
 */
public class Message {

    private final String messageId;
    private final String sender;
    private final String content;
    private final String roomName;
    private final MessageType type;
    private final Instant timestamp;

    public Message(String sender, String content, String roomName, MessageType type) {
        this.messageId = UUID.randomUUID().toString().substring(0, 8);
        this.sender = sender;
        this.content = content;
        this.roomName = roomName;
        this.type = type;
        this.timestamp = Instant.now();
    }

    // --- Getters (no setters — immutable) ---

    public String getMessageId() {
        return messageId;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getRoomName() {
        return roomName;
    }

    public MessageType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Message[%s|%s|%s|%s]", type, sender, roomName, content);
    }
}
