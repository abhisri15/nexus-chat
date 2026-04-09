package com.nexuschat.exception;

/**
 * Base unchecked exception for all NexusChat errors.
 */
public class ChatException extends RuntimeException {

    public ChatException(String message) {
        super(message);
    }

    public ChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
