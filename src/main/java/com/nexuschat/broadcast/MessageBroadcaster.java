package com.nexuschat.broadcast;

import com.nexuschat.client.ConnectedClient;
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
        // TODO: while (running)
        //       1. message = messageQueue.dequeue(callback)
        //          → This BLOCKS when queue is empty (consumer waits)
        //       2. If message is null (shutdown), break
        //       3. Notify observer: onMessageBroadcast(message, room)
        //       4. broadcastToMembers(message, room.getMembers())
        //
        // Catch InterruptedException → log, restore interrupt flag, exit
    }

    /**
     * Stop the broadcaster. Called during room/server shutdown.
     */
    public void stop() {
        // TODO: Set running = false
    }

    /**
     * Fan-out: deliver message to every member in the room.
     *
     * Uses ChatProtocol.formatForDisplay() to create the display string,
     * then writes to each client. If a write fails, delegates to
     * BackpressureHandler.
     */
    private void broadcastToMembers(Message message, List<ConnectedClient> members) {
        // TODO: String formatted = ChatProtocol.formatForDisplay(message)
        //       for each member:
        //         deliverToClient(member, formatted)
    }

    /**
     * Deliver a formatted message to a single client.
     * Handles write failures via BackpressureHandler.
     */
    private void deliverToClient(ConnectedClient client, String formatted) {
        // TODO: try: client.sendMessage(formatted)
        //       catch Exception:
        //         action = backpressureHandler.handleSlowClient(client, message)
        //         switch(action):
        //           DROP_MESSAGE → skip (already logged by handler)
        //           DISCONNECT_CLIENT → client.disconnect(), room.leave(client)
        //           RETRY_ONCE → try once more, then drop
    }
}
