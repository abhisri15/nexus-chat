package com.nexuschat.room;

import com.nexuschat.broadcast.BackpressureHandler;
import com.nexuschat.broadcast.DropMessageHandler;
import com.nexuschat.broadcast.MessageBroadcaster;
import com.nexuschat.client.ConnectedClient;
import com.nexuschat.message.Message;
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

    private final String name;
    private final CopyOnWriteArrayList<ConnectedClient> members = new CopyOnWriteArrayList<>();
    private final BoundedMessageQueue messageQueue;
    private final MessageBroadcaster broadcaster;
    private final Thread broadcasterThread;
    private final RoomEventListener eventListener;
    private final AtomicInteger messageCount = new AtomicInteger(0);

    public Room(String name, int queueCapacity, RoomEventListener eventListener) {
        this.name = name;
        this.eventListener = eventListener;
        this.messageQueue = new RoomMessageQueue(queueCapacity);

        BackpressureHandler backpressureHandler = new DropMessageHandler();
        this.broadcaster = new MessageBroadcaster(messageQueue, this, backpressureHandler, eventListener);
        this.broadcasterThread = new Thread(broadcaster, "broadcaster-" + name);
    }

    /**
     * Start the broadcaster consumer thread. Call once after room creation.
     */
    public void startBroadcaster() {
        // TODO: Start the broadcaster thread (daemon thread)
    }

    /**
     * Stop the broadcaster and drain the queue. Call when room is destroyed.
     */
    public void stopBroadcaster() {
        // TODO: Stop broadcaster, shutdown queue, join broadcaster thread
    }

    /**
     * Add a client to this room.
     * Called from ClientHandler thread — CopyOnWriteArrayList handles concurrency.
     */
    public void join(ConnectedClient client) {
        // TODO: Add to members list
        //       Set client's current room to this
        //       Notify observer: onClientJoined
        //       Submit a JOIN message to the queue (so it's announced in order)
    }

    /**
     * Remove a client from this room.
     * Called from ClientHandler thread.
     */
    public void leave(ConnectedClient client) {
        // TODO: Remove from members list
        //       Set client's current room to null
        //       Notify observer: onClientLeft
        //       Submit a LEAVE message to the queue
    }

    /**
     * Submit a message to this room's queue.
     * Called by ClientHandler (PRODUCER) — may BLOCK if queue is full.
     */
    public void submitMessage(Message message) {
        // TODO: Call messageQueue.enqueue(message, callback)
        //       Callback increments messageCount
        //       Handle InterruptedException
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public List<ConnectedClient> getMembers() {
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
}
