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
        // TODO: 1. Send welcome message, prompt for username
        // TODO: 2. Read username, validate (not taken, not blank)
        // TODO: 3. Register in ClientRegistry
        // TODO: 4. Enter message loop:
        //          - line = client.readLine()
        //          - if null → client disconnected, break
        //          - if command → handleCommand(line)
        //          - else → handleChatMessage(line)
        // TODO: 5. Finally block: handleDisconnect()
    }

    /**
     * Route a command string to the appropriate handler.
     */
    private void handleCommand(String input) {
        // TODO: Parse command with ChatProtocol.parseCommand(input)
        //       Switch on command name:
        //         "join"  → handleJoin(args)
        //         "leave" → handleLeave()
        //         "rooms" → handleListRooms()
        //         "users" → handleListUsers()
        //         "quit"  → handleQuit()
        //         default → sendSystemMessage("Unknown command")
    }

    /**
     * Join a room (create if it doesn't exist).
     * If already in a room, leave it first.
     */
    private void handleJoin(String roomName) {
        // TODO: If client is already in a room, call handleLeave() first
        //       Get or create room via roomManager
        //       Call room.join(client)
        //       Send system message confirming join
    }

    /**
     * Leave the current room.
     */
    private void handleLeave() {
        // TODO: Check if client is in a room
        //       Call room.leave(client)
        //       Send confirmation
    }

    /**
     * List all active rooms and their member counts.
     */
    private void handleListRooms() {
        // TODO: Get room info from roomManager.getRoomInfo()
        //       Format and send to client
    }

    /**
     * List all users in the client's current room.
     */
    private void handleListUsers() {
        // TODO: Check if in a room
        //       Get members from room.getMembers()
        //       Format usernames and send to client
    }

    /**
     * Client requested disconnect.
     */
    private void handleQuit() {
        // TODO: Send goodbye message
        //       Trigger disconnect
    }

    /**
     * Wrap text in a Message and enqueue to the room's bounded queue.
     * THIS IS THE PRODUCER ACTION — enqueue blocks if queue is full (backpressure).
     */
    private void handleChatMessage(String content) {
        // TODO: Check if client is in a room (send error if not)
        //       Create Message(username, content, roomName, CHAT)
        //       Call room.submitMessage(message)
        //       ^^^ This calls queue.enqueue() which BLOCKS if full
    }

    /**
     * Clean up on disconnect (graceful or abrupt).
     */
    private void handleDisconnect() {
        // TODO: If in a room → leave it
        //       Unregister from ClientRegistry
        //       Disconnect the client socket
    }

    /**
     * Send a system message directly to this client (not through the queue).
     */
    private void sendSystemMessage(String text) {
        // TODO: client.sendMessage(ChatProtocol.formatSystemMessage(text))
    }
}
