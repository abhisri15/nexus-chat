package com.nexuschat.client;

import com.nexuschat.message.ChatProtocol;
import com.nexuschat.message.Message;
import com.nexuschat.message.MessageType;
import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.room.Room;
import com.nexuschat.room.RoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PRODUCER role in the producer-consumer pattern.
 *
 * One ClientHandler runs per connected client on a thread pool thread.
 * It reads lines from the client socket, parses them, and either:
 *   - Executes a command (/join, /leave, /rooms, /users, /quit)
 *   - Wraps the text in a Message and enqueues it to the room's bounded queue
 *
 * This is the "producer" — it PRODUCES messages into the room's queue.
 * The MessageBroadcaster is the "consumer" that dequeues and delivers them.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final ConnectedClient client;
    private final RoomManager roomManager;
    private final ClientRegistry clientRegistry;
    private final RoomEventListener eventListener;

    public ClientHandler(ConnectedClient client, RoomManager roomManager,
                         ClientRegistry clientRegistry, RoomEventListener eventListener) {
        this.client = client;
        this.roomManager = roomManager;
        this.clientRegistry = clientRegistry;
        this.eventListener = eventListener;
    }

    @Override
    public void run() {
        try {
            sendSystemMessage("Welcome to NexusChat! Enter your username:");

            // Username registration loop
            String username;
            while (true) {
                username = client.readLine();
                if (username == null) return;
                username = username.trim();
                if (username.isBlank()) {
                    sendSystemMessage("Username cannot be blank. Try again:");
                    continue;
                }
                client.setUsername(username);
                if (clientRegistry.register(client)) break;

                client.setUsername(null);
                sendSystemMessage("Username '" + username + "' is taken. Try another:");
            }

            sendSystemMessage("Hello " + username + "! Commands: /join <room>, /leave, /rooms, /users, /quit");

            // Message loop
            String line;
            while ((line = client.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (ChatProtocol.isCommand(line)) {
                    handleCommand(line);
                } else {
                    handleChatMessage(line);
                }
            }
        } catch (Exception e) {
            eventListener.onError("ClientHandler", e);
        } finally {
            handleDisconnect();
        }
    }

    /**
     * Route a command string to the appropriate handler.
     */
    private void handleCommand(String input) {
        String[] parts = ChatProtocol.parseCommand(input);
        if (parts.length == 0) return;

        switch (parts[0].toLowerCase()) {
            case "join" -> {
                if (parts.length < 2) {
                    sendSystemMessage("Usage: /join <roomName>");
                } else {
                    handleJoin(parts[1]);
                }
            }
            case "leave" -> handleLeave();
            case "rooms" -> handleListRooms();
            case "users" -> handleListUsers();
            case "quit"  -> handleQuit();
            default      -> sendSystemMessage("Unknown command: /" + parts[0]);
        }
    }

    /**
     * Join a room (create if it doesn't exist).
     * If already in a room, leave it first.
     */
    private void handleJoin(String roomName) {
        if (client.getCurrentRoom() != null) {
            handleLeave();
        }
        Room room = roomManager.getOrCreateRoom(roomName);
        room.join(client);
        sendSystemMessage("Joined #" + roomName);
    }

    /**
     * Leave the current room.
     */
    private void handleLeave() {
        Room room = client.getCurrentRoom();
        if (room == null) {
            sendSystemMessage("You're not in any room.");
            return;
        }
        room.leave(client);
        sendSystemMessage("Left #" + room.getName());
    }

    /**
     * List all active rooms and their member counts.
     */
    private void handleListRooms() {
        var roomInfo = roomManager.getRoomInfo();
        if (roomInfo.isEmpty()) {
            sendSystemMessage("No active rooms. Create one with /join <name>");
            return;
        }
        StringBuilder sb = new StringBuilder("Active rooms:\n");
        roomInfo.forEach((name, count) -> sb.append("  #").append(name).append(" (").append(count).append(" users)\n"));
        sendSystemMessage(sb.toString().trim());
    }

    /**
     * List all users in the client's current room.
     */
    private void handleListUsers() {
        Room room = client.getCurrentRoom();
        if (room == null) {
            sendSystemMessage("You're not in any room.");
            return;
        }
        StringBuilder sb = new StringBuilder("Users in #" + room.getName() + ":\n");
        for (ConnectedClient member : room.getMembers()) {
            sb.append("  - ").append(member.getUsername()).append("\n");
        }
        sendSystemMessage(sb.toString().trim());
    }

    /**
     * Client requested disconnect.
     */
    private void handleQuit() {
        sendSystemMessage("Goodbye!");
        client.disconnect();
    }

    /**
     * Wrap text in a Message and enqueue to the room's bounded queue.
     * THIS IS THE PRODUCER ACTION — enqueue blocks if queue is full (backpressure).
     */
    private void handleChatMessage(String content) {
        Room room = client.getCurrentRoom();
        if (room == null) {
            sendSystemMessage("Join a room first with /join <name>");
            return;
        }
        Message message = new Message(client.getUsername(), content, room.getName(), MessageType.CHAT);
        room.submitMessage(message);
    }

    /**
     * Clean up on disconnect (graceful or abrupt).
     */
    private void handleDisconnect() {
        if (client.getCurrentRoom() != null) {
            client.getCurrentRoom().leave(client);
        }
        clientRegistry.unregister(client.getClientId());
        client.disconnect();
        logger.info("Client '{}' disconnected", client.getUsername());
    }

    /**
     * Send a system message directly to this client (not through the queue).
     */
    private void sendSystemMessage(String text) {
        client.sendMessage(ChatProtocol.formatSystemMessage(text));
    }
}
