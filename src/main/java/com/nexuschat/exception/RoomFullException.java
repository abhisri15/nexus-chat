package com.nexuschat.exception;

/**
 * Thrown when a room's member list exceeds its maximum capacity.
 */
public class RoomFullException extends ChatException {

    public RoomFullException(String roomName) {
        super("Room '#" + roomName + "' is full");
    }
}
