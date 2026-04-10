package com.nexuschat.room;

import com.nexuschat.message.Message;
import com.nexuschat.message.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe circular buffer that retains the last N chat messages per room.
 *
 * The broadcaster thread calls add() after each successful broadcast;
 * the WebSocket server calls getRecent() when a client joins a room.
 * Both operations are synchronized on the internal array to avoid
 * torn reads during concurrent add + getRecent.
 */
public class MessageHistory {

    private final Message[] buffer;
    private int head = 0;
    private int size = 0;

    public MessageHistory(int capacity) {
        this.buffer = new Message[capacity];
    }

    public synchronized void add(Message message) {
        if (message.getType() != MessageType.CHAT) return;
        buffer[head] = message;
        head = (head + 1) % buffer.length;
        if (size < buffer.length) size++;
    }

    public synchronized List<Message> getRecent() {
        List<Message> result = new ArrayList<>(size);
        int start = (head - size + buffer.length) % buffer.length;
        for (int i = 0; i < size; i++) {
            result.add(buffer[(start + i) % buffer.length]);
        }
        return result;
    }
}
