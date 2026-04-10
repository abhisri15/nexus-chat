package com.nexuschat.client;

import com.nexuschat.room.Room;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

/**
 * Represents a single connected client.
 *
 * Wraps a TCP Socket with buffered I/O. Provides thread-safe sendMessage()
 * (synchronized on the writer) since both the MessageBroadcaster and
 * ClientHandler may write to the same client.
 *
 * Concurrency contract:
 * - readLine() is called ONLY by the owning ClientHandler thread
 * - sendMessage() may be called from ANY thread (synchronized)
 * - disconnect() may be called from ANY thread (idempotent)
 */
public class ConnectedClient {

    private final String clientId;
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private String username;
    private Room currentRoom;
    private volatile boolean connected;

    public ConnectedClient(Socket socket) throws IOException {
        this.clientId = UUID.randomUUID().toString().substring(0, 8);
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.connected = true;
    }

    /**
     * Send a message to this client. Thread-safe — synchronized on writer
     * because broadcaster thread and system messages can call concurrently.
     */
    public void sendMessage(String rawMessage) {
        synchronized (writer) {
            if (!connected) return;
            writer.println(rawMessage);
            writer.flush();
        }
    }

    /**
     * Blocking read from client socket.
     * Called ONLY by the owning ClientHandler thread.
     */
    public String readLine() throws IOException {
        return reader.readLine();
    }

    /**
     * Close the socket and mark as disconnected. Idempotent.
     */
    public void disconnect() {
        connected = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    // --- Getters / Setters ---

    public String getClientId() {
        return clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }

    public boolean isConnected() {
        return connected;
    }
}
