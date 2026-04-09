package com.nexuschat.broadcast;

import com.nexuschat.client.ConnectedClient;
import com.nexuschat.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default backpressure strategy: drop the message for the slow client.
 *
 * This is the safest default — one slow client doesn't affect others.
 * The dropped message is logged for observability.
 */
public class DropMessageHandler implements BackpressureHandler {

    private static final Logger logger = LoggerFactory.getLogger(DropMessageHandler.class);

    @Override
    public SlowClientAction handleSlowClient(ConnectedClient client, Message message) {
        // TODO: Log a warning with client username and message ID
        //       Return DROP_MESSAGE
        return SlowClientAction.DROP_MESSAGE;
    }
}
