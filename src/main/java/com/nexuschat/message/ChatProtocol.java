package com.nexuschat.message;

/**
 * Wire protocol for client-server communication.
 *
 * Encoding format (pipe-delimited, one message per line):
 *   TYPE|sender|roomName|content
 *
 * Examples:
 *   CHAT|abhikalp|general|Hello everyone!
 *   JOIN|abhikalp|general|
 *   SYSTEM|server||Welcome to NexusChat
 *
 * Also handles command parsing — lines starting with "/" are commands.
 */
public class ChatProtocol {

    public static final String DELIMITER = "\\|";
    public static final String PIPE = "|";
    public static final String COMMAND_PREFIX = "/";

    /**
     * Encode a Message into wire format string.
     * Format: TYPE|sender|roomName|content
     */
    public static String encode(Message message) {
        return message.getType() + PIPE + message.getSender() + PIPE
                + message.getRoomName() + PIPE + message.getContent();
    }

    /**
     * Decode a wire format string into a Message.
     * Returns null if the format is invalid.
     */
    public static Message decode(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String[] parts = raw.split(DELIMITER, 4);
        if (parts.length < 4) return null;

        try {
            MessageType type = MessageType.valueOf(parts[0]);
            return new Message(parts[1], parts[3], parts[2], type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Format a Message for display on the client terminal.
     *
     * CHAT:   [#general] abhikalp: Hello everyone!
     * JOIN:   [#general] >> abhikalp joined the room
     * LEAVE:  [#general] << abhikalp left the room
     * SYSTEM: [SERVER] Welcome to NexusChat
     */
    public static String formatForDisplay(Message message) {
        return switch (message.getType()) {
            case CHAT -> "[#" + message.getRoomName() + "] " + message.getSender() + ": " + message.getContent();
            case JOIN -> "[#" + message.getRoomName() + "] >> " + message.getSender() + " joined the room";
            case LEAVE -> "[#" + message.getRoomName() + "] << " + message.getSender() + " left the room";
            case SYSTEM -> "[SERVER] " + message.getContent();
            case BROADCAST -> "[BROADCAST] " + message.getContent();
        };
    }

    /**
     * Format a system message string (not a full Message object).
     * Used for direct server-to-client communication.
     */
    public static String formatSystemMessage(String text) {
        return "[SERVER] " + text;
    }

    /**
     * Check if a raw input line is a command (starts with "/").
     */
    public static boolean isCommand(String input) {
        return input != null && input.startsWith(COMMAND_PREFIX);
    }

    /**
     * Parse a command string into [commandName, ...args].
     * Example: "/join general" → ["join", "general"]
     */
    public static String[] parseCommand(String input) {
        return input.substring(1).trim().split("\\s+");
    }
}
