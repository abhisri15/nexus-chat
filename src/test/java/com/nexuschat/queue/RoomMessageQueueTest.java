package com.nexuschat.queue;

import com.nexuschat.message.Message;
import com.nexuschat.message.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RoomMessageQueueTest {

    private RoomMessageQueue queue;

    private Message msg(String content) {
        return new Message("user", content, "room", MessageType.CHAT);
    }

    @BeforeEach
    void setUp() {
        queue = new RoomMessageQueue(3);
    }

    @Test
    void enqueueAndDequeue_fifoOrder() throws InterruptedException {
        queue.enqueue(msg("first"), (m, s) -> {});
        queue.enqueue(msg("second"), (m, s) -> {});

        Message out1 = queue.dequeue((m, s) -> {});
        Message out2 = queue.dequeue((m, s) -> {});

        assertEquals("first", out1.getContent());
        assertEquals("second", out2.getContent());
    }

    @Test
    void sizeAndCapacity_trackCorrectly() throws InterruptedException {
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        assertFalse(queue.isFull());
        assertEquals(3, queue.getCapacity());

        queue.enqueue(msg("a"), (m, s) -> {});
        queue.enqueue(msg("b"), (m, s) -> {});
        queue.enqueue(msg("c"), (m, s) -> {});

        assertEquals(3, queue.size());
        assertTrue(queue.isFull());
        assertFalse(queue.isEmpty());
    }

    @Test
    void callback_receivesAccurateSizeInsideLock() throws InterruptedException {
        AtomicInteger enqueueSize = new AtomicInteger(-1);
        AtomicInteger dequeueSize = new AtomicInteger(-1);

        queue.enqueue(msg("a"), (m, s) -> {});
        queue.enqueue(msg("b"), (m, size) -> enqueueSize.set(size));

        assertEquals(2, enqueueSize.get());

        queue.dequeue((m, size) -> dequeueSize.set(size));
        assertEquals(1, dequeueSize.get());
    }

    @Test
    void producerBlocks_whenQueueFull() throws InterruptedException {
        queue.enqueue(msg("1"), (m, s) -> {});
        queue.enqueue(msg("2"), (m, s) -> {});
        queue.enqueue(msg("3"), (m, s) -> {});

        CountDownLatch producerStarted = new CountDownLatch(1);
        CountDownLatch producerDone = new CountDownLatch(1);

        Thread producer = new Thread(() -> {
            try {
                producerStarted.countDown();
                queue.enqueue(msg("4"), (m, s) -> {});
                producerDone.countDown();
            } catch (InterruptedException ignored) {}
        });
        producer.start();
        producerStarted.await();

        // Producer should be blocked — not done yet
        assertFalse(producerDone.await(200, TimeUnit.MILLISECONDS));

        // Dequeue one to unblock producer
        queue.dequeue((m, s) -> {});
        assertTrue(producerDone.await(1, TimeUnit.SECONDS));
    }

    @Test
    void consumerBlocks_whenQueueEmpty() throws InterruptedException {
        CountDownLatch consumerStarted = new CountDownLatch(1);
        CountDownLatch consumerDone = new CountDownLatch(1);
        AtomicReference<Message> received = new AtomicReference<>();

        Thread consumer = new Thread(() -> {
            try {
                consumerStarted.countDown();
                received.set(queue.dequeue((m, s) -> {}));
                consumerDone.countDown();
            } catch (InterruptedException ignored) {}
        });
        consumer.start();
        consumerStarted.await();

        // Consumer should be blocked
        assertFalse(consumerDone.await(200, TimeUnit.MILLISECONDS));

        // Enqueue to unblock
        queue.enqueue(msg("hello"), (m, s) -> {});
        assertTrue(consumerDone.await(1, TimeUnit.SECONDS));
        assertEquals("hello", received.get().getContent());
    }

    @Test
    void shutdown_unblockWaitingConsumer() throws InterruptedException {
        CountDownLatch consumerDone = new CountDownLatch(1);
        AtomicReference<Message> received = new AtomicReference<>();

        Thread consumer = new Thread(() -> {
            try {
                received.set(queue.dequeue((m, s) -> {}));
                consumerDone.countDown();
            } catch (InterruptedException ignored) {}
        });
        consumer.start();

        Thread.sleep(100);
        queue.shutdown();

        assertTrue(consumerDone.await(1, TimeUnit.SECONDS));
        assertNull(received.get());
    }

    @Test
    void shutdown_unblockWaitingProducer() throws InterruptedException {
        queue.enqueue(msg("1"), (m, s) -> {});
        queue.enqueue(msg("2"), (m, s) -> {});
        queue.enqueue(msg("3"), (m, s) -> {});

        CountDownLatch producerDone = new CountDownLatch(1);

        Thread producer = new Thread(() -> {
            try {
                queue.enqueue(msg("4"), (m, s) -> {});
                producerDone.countDown();
            } catch (InterruptedException ignored) {}
        });
        producer.start();

        Thread.sleep(100);
        queue.shutdown();

        assertTrue(producerDone.await(1, TimeUnit.SECONDS));
        // Queue should still have the original 3 (the 4th was skipped due to shutdown)
        assertEquals(3, queue.size());
    }

    @Test
    void queueStatus_formatsCorrectly() throws InterruptedException {
        assertEquals("Queue[0/3]", queue.getQueueStatus());
        queue.enqueue(msg("a"), (m, s) -> {});
        assertEquals("Queue[1/3]", queue.getQueueStatus());
    }
}
