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
        // TODO: synchronized(monitor)
        //       - while (queue full AND active) → monitor.wait()
        //       - if (!active) return (shutdown was called)
        //       - queue.addLast(message)
        //       - callback.accept(message, queue.size())
        //       - monitor.notifyAll() → wake consumer
    }

    @Override
    public Message dequeue(BiConsumer<Message, Integer> callback) throws InterruptedException {
        // TODO: synchronized(monitor)
        //       - while (queue empty AND active) → monitor.wait()
        //       - if (!active && queue.isEmpty()) return null (shutdown)
        //       - message = queue.pollFirst()
        //       - callback.accept(message, queue.size())
        //       - monitor.notifyAll() → wake producer
        //       - return message
        return null;
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
        // TODO: Set active = false
        //       synchronized(monitor) → monitor.notifyAll()
        //       This wakes any blocked producer/consumer so they can exit
    }
}
