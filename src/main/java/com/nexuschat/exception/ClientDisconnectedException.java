package com.nexuschat.exception;

/**
 * Thrown when attempting to communicate with a disconnected client.
 */
public class ClientDisconnectedException extends ChatException {

    public ClientDisconnectedException(String clientId) {
        super("Client '" + clientId + "' is disconnected");
    }
}
