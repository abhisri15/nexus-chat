package com.nexuschat.queue;

import com.nexuschat.message.Message;

import java.util.function.BiConsumer;

/**
 * Strategy interface for the bounded message queue.
 *
 * This is the CORE of the producer-consumer pattern:
 * - Producers (ClientHandler) call enqueue() — blocks when full
 * - Consumers (MessageBroadcaster) call dequeue() — blocks when empty
 *
 * The BiConsumer callback is invoked INSIDE the synchronized block,
 * guaranteeing accurate queue size at the time of the operation
 * (same pattern as SharedQueue from the assignment).
 *
 * Strategy pattern: swap RoomMessageQueue for a different implementation
 * (e.g., lock-free, priority-based) without changing producers/consumers.
 */
public interface BoundedMessageQueue {

    /**
     * Enqueue a message. BLOCKS if queue is at capacity (backpressure).
     * Callback receives (message, currentQueueSize) inside the critical section.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void enqueue(Message message, BiConsumer<Message, Integer> callback) throws InterruptedException;

    /**
     * Dequeue a message. BLOCKS if queue is empty.
     * Callback receives (message, currentQueueSize) inside the critical section.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    Message dequeue(BiConsumer<Message, Integer> callback) throws InterruptedException;

    int size();

    boolean isEmpty();

    boolean isFull();

    int getCapacity();

    String getQueueStatus();

    /**
     * Signal shutdown — wake any blocked threads so they can exit.
     */
    void shutdown();
}
