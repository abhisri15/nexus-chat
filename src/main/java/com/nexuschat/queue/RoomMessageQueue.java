package com.nexuschat.queue;

import com.nexuschat.message.Message;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;

/**
 * Bounded message queue implementation using wait/notifyAll.
 *
 * Direct evolution of SharedQueue from the producer-consumer assignment.
 * One instance per Room — isolates backpressure between rooms.
 *
 * Concurrency contract:
 * - enqueue() blocks when queue.size() >= capacity (producer waits)
 * - dequeue() blocks when queue is empty (consumer waits)
 * - Callback is invoked INSIDE synchronized block for accurate size
 * - shutdown() sets active=false and wakes all blocked threads
 *
 * Monitor pattern: single monitor object, synchronized + wait + notifyAll
 */
public class RoomMessageQueue implements BoundedMessageQueue {

    private final int capacity;
    private final Object monitor = new Object();
    private final Deque<Message> queue = new ArrayDeque<>();
    private volatile boolean active = true;

    public RoomMessageQueue(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void enqueue(Message message, BiConsumer<Message, Integer> callback) throws InterruptedException {
        synchronized (monitor) {
            while (queue.size() >= capacity && active) {
                monitor.wait();
            }
            if (!active) return;

            queue.addLast(message);
            int size = queue.size();
            callback.accept(message, size);
            monitor.notifyAll();
        }
    }

    @Override
    public Message dequeue(BiConsumer<Message, Integer> callback) throws InterruptedException {
        synchronized (monitor) {
            while (queue.isEmpty() && active) {
                monitor.wait();
            }
            if (!active && queue.isEmpty()) return null;

            Message message = queue.pollFirst();
            int size = queue.size();
            callback.accept(message, size);
            monitor.notifyAll();
            return message;
        }
    }

    @Override
    public int size() {
        synchronized (monitor) {
            return queue.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (monitor) {
            return queue.isEmpty();
        }
    }

    @Override
    public boolean isFull() {
        synchronized (monitor) {
            return queue.size() >= capacity;
        }
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public String getQueueStatus() {
        synchronized (monitor) {
            return String.format("Queue[%d/%d]", queue.size(), capacity);
        }
    }

    @Override
    public void shutdown() {
        active = false;
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }
}
