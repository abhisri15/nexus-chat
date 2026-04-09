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
        // TODO: Concatenate fields with PIPE delimiter
        return null;
    }

    /**
     * Decode a wire format string into a Message.
     * Returns null if the format is invalid.
     */
    public static Message decode(String raw) {
        // TODO: Split by DELIMITER
        //       Validate at least 4 parts
        //       Parse MessageType from parts[0]
        //       Create and return Message
        return null;
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
        // TODO: Switch on message type, format accordingly
        return null;
    }

    /**
     * Format a system message string (not a full Message object).
     * Used for direct server-to-client communication.
     */
    public static String formatSystemMessage(String text) {
        // TODO: Return something like "[SERVER] " + text
        return null;
    }

    /**
     * Check if a raw input line is a command (starts with "/").
     */
    public static boolean isCommand(String input) {
        // TODO: Check prefix
        return false;
    }

    /**
     * Parse a command string into [commandName, ...args].
     * Example: "/join general" → ["join", "general"]
     */
    public static String[] parseCommand(String input) {
        // TODO: Strip "/" prefix, split by whitespace
        return null;
    }
}
