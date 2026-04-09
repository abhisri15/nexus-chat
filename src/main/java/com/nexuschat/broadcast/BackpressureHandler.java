package com.nexuschat.broadcast;

import com.nexuschat.client.ConnectedClient;
import com.nexuschat.message.Message;

/**
 * Strategy interface for handling slow or unresponsive clients.
 *
 * When the MessageBroadcaster tries to deliver a message to a client
 * and the write fails (IOException, timeout), it delegates to this handler.
 *
 * Strategy pattern: swap implementations without changing broadcaster logic.
 */
public interface BackpressureHandler {

    /**
     * Decide what to do with a slow client.
     *
     * @param client  the client that couldn't receive the message
     * @param message the message that failed to deliver
     * @return the action to take
     */
    SlowClientAction handleSlowClient(ConnectedClient client, Message message);
}
