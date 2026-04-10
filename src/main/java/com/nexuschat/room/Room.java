package com.nexuschat.room;

import com.nexuschat.broadcast.BackpressureHandler;
import com.nexuschat.broadcast.DropMessageHandler;
import com.nexuschat.broadcast.MessageBroadcaster;
import com.nexuschat.client.ChatClient;
import com.nexuschat.message.Message;
import com.nexuschat.message.MessageType;
import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.queue.BoundedMessageQueue;
import com.nexuschat.queue.RoomMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A chat room — the SHARED RESOURCE in this concurrent system.
 *
 * Multiple ClientHandler threads (producers) call submitMessage() concurrently.
 * A single MessageBroadcaster thread (consumer) dequeues and delivers.
 * Clients join/leave from their handler threads while the broadcaster iterates.
 *
 * Concurrency:
 * - members: CopyOnWriteArrayList — safe iteration during broadcast while
 *   other threads join/leave. Reads >> writes, so COW is ideal.
 * - messageQueue: BoundedMessageQueue — synchronized internally
 * - messageCount: AtomicInteger — lock-free counter
 */
public class Room {

    private static final Logger logger = LoggerFactory.getLogger(Room.class);

    private static final int HISTORY_CAPACITY = 50;

    private final String name;
    private final CopyOnWriteArrayList<ChatClient> members = new CopyOnWriteArrayList<>();
    private final BoundedMessageQueue messageQueue;
    private final MessageBroadcaster broadcaster;
    private final Thread broadcasterThread;
    private final RoomEventListener eventListener;
    private final MessageHistory history;
    private final AtomicInteger messageCount = new AtomicInteger(0);
    private volatile boolean active = true;

    public Room(String name, int queueCapacity, RoomEventListener eventListener) {
        this.name = name;
        this.eventListener = eventListener;
        this.messageQueue = new RoomMessageQueue(queueCapacity);
        this.history = new MessageHistory(HISTORY_CAPACITY);

        BackpressureHandler backpressureHandler = new DropMessageHandler();
        this.broadcaster = new MessageBroadcaster(messageQueue, this, backpressureHandler, eventListener);
        this.broadcasterThread = new Thread(broadcaster, "broadcaster-" + name);
    }

    /**
     * Start the broadcaster consumer thread. Call once after room creation.
     */
    public void startBroadcaster() {
        broadcasterThread.setDaemon(true);
        broadcasterThread.start();
    }

    /**
     * Stop the broadcaster and drain the queue. Call when room is destroyed.
     */
    public void stopBroadcaster() {
        active = false;
        broadcaster.stop();
        messageQueue.shutdown();
        try {
            broadcasterThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Add a client to this room.
     * Called from ClientHandler thread — CopyOnWriteArrayList handles concurrency.
     */
    public void join(ChatClient client) {
        if (!active) return;
        members.add(client);
        client.setCurrentRoom(this);
        eventListener.onClientJoined(client, this);
        submitMessage(new Message(client.getUsername(), "", name, MessageType.JOIN));
    }

    /**
     * Remove a client from this room.
     * Called from ClientHandler thread OR broadcaster thread (on slow client disconnect).
     *
     * Guard: only removes from members and clears room ref.
     * LEAVE announcement is best-effort — if queue is shut down, skip it.
     */
    public void leave(ChatClient client) {
        if (!members.remove(client)) return;
        client.setCurrentRoom(null);
        eventListener.onClientLeft(client, this);
        if (active) {
            submitMessage(new Message(client.getUsername(), "", name, MessageType.LEAVE));
        }
    }

    /**
     * Submit a message to this room's queue.
     * Called by ClientHandler (PRODUCER) — may BLOCK if queue is full.
     */
    public void submitMessage(Message message) {
        if (!active) return;
        try {
            messageQueue.enqueue(message, (msg, size) -> messageCount.incrementAndGet());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while submitting message to #{}", name);
        }
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public List<ChatClient> getMembers() {
        return members;
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getMessageCount() {
        return messageCount.get();
    }

    public String getQueueStatus() {
        return messageQueue.getQueueStatus();
    }

    public MessageHistory getHistory() {
        return history;
    }
}
