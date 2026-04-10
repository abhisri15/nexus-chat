package com.nexuschat.broadcast;

import com.nexuschat.client.ChatClient;
import com.nexuschat.message.ChatProtocol;
import com.nexuschat.message.Message;
import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.queue.BoundedMessageQueue;
import com.nexuschat.room.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CONSUMER role in the producer-consumer pattern.
 *
 * One MessageBroadcaster runs per Room on its own thread.
 * It continuously dequeues messages from the room's BoundedMessageQueue
 * and fans them out to all room members.
 *
 * This is the "consumer" — it CONSUMES messages from the queue
 * and delivers them to connected clients.
 *
 * Fan-out pattern: one dequeued message → N writes (one per member).
 * If a write fails, the BackpressureHandler decides the action.
 */
public class MessageBroadcaster implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MessageBroadcaster.class);

    private final BoundedMessageQueue messageQueue;
    private final Room room;
    private final BackpressureHandler backpressureHandler;
    private final RoomEventListener eventListener;
    private volatile boolean running;

    public MessageBroadcaster(BoundedMessageQueue messageQueue, Room room,
                              BackpressureHandler backpressureHandler,
                              RoomEventListener eventListener) {
        this.messageQueue = messageQueue;
        this.room = room;
        this.backpressureHandler = backpressureHandler;
        this.eventListener = eventListener;
        this.running = true;
    }

    @Override
    public void run() {
        logger.info("Broadcaster started for #{}", room.getName());
        try {
            while (running) {
                Message message = messageQueue.dequeue((msg, size) -> {});
                if (message == null) break;

                eventListener.onMessageBroadcast(message, room);
                broadcastToMembers(message, room.getMembers());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Broadcaster interrupted for #{}", room.getName());
        }
        logger.info("Broadcaster stopped for #{}", room.getName());
    }

    /**
     * Stop the broadcaster. Called during room/server shutdown.
     */
    public void stop() {
        running = false;
    }

    /**
     * Fan-out: deliver message to every member in the room.
     *
     * Uses ChatProtocol.formatForDisplay() to create the display string,
     * then writes to each client. If a write fails, delegates to
     * BackpressureHandler.
     */
    private void broadcastToMembers(Message message, List<ChatClient> members) {
        String formatted = ChatProtocol.formatForDisplay(message);
        for (ChatClient member : members) {
            deliverToClient(member, formatted, message);
        }
    }

    /**
     * Deliver a formatted message to a single client.
     * Handles write failures via BackpressureHandler.
     *
     * IMPORTANT: Never call room.leave() from the broadcaster thread —
     * leave() enqueues a LEAVE message to the same queue we're consuming,
     * which would deadlock. Instead, just disconnect the client; its
     * ClientHandler's finally block will call leave() on its own thread.
     */
    private void deliverToClient(ChatClient client, String formatted, Message message) {
        if (!client.isConnected()) return;

        try {
            client.sendMessage(formatted);
        } catch (Exception e) {
            SlowClientAction action = backpressureHandler.handleSlowClient(client, message);
            switch (action) {
                case DISCONNECT_CLIENT -> client.disconnect();
                case RETRY_ONCE -> {
                    try {
                        client.sendMessage(formatted);
                    } catch (Exception retryEx) {
                        backpressureHandler.handleSlowClient(client, message);
                    }
                }
                case DROP_MESSAGE -> { /* already logged by handler */ }
            }
        }
    }
}
