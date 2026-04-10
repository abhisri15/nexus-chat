package com.nexuschat.websocket;

import com.nexuschat.client.ChatClient;
import com.nexuschat.client.ClientRegistry;
import com.nexuschat.message.Message;
import com.nexuschat.message.MessageType;
import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.room.Room;
import com.nexuschat.room.RoomManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket server that bridges browser clients to the NexusChat system.
 *
 * Uses JSON messages for browser-friendly communication.
 * Shares the same RoomManager and ClientRegistry as the TCP server,
 * so TCP and WebSocket clients can chat together.
 */
public class WebSocketChatServer extends WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatServer.class);

    private final RoomManager roomManager;
    private final ClientRegistry clientRegistry;
    private final RoomEventListener eventListener;
    private final ConcurrentHashMap<WebSocket, WebSocketChatClient> clients = new ConcurrentHashMap<>();

    public WebSocketChatServer(int port, RoomManager roomManager,
                               ClientRegistry clientRegistry, RoomEventListener eventListener) {
        super(new InetSocketAddress(port));
        this.roomManager = roomManager;
        this.clientRegistry = clientRegistry;
        this.eventListener = eventListener;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        WebSocketChatClient client = new WebSocketChatClient(conn);
        clients.put(conn, client);
        logger.info("WebSocket client connected: {}", client.getClientId());
    }

    @Override
    public void onMessage(WebSocket conn, String raw) {
        WebSocketChatClient client = clients.get(conn);
        if (client == null) return;

        try {
            // Simple JSON parsing without a library
            String type = extractJsonField(raw, "type");
            if (type == null) return;

            switch (type) {
                case "auth" -> handleAuth(client, raw);
                case "join" -> handleJoin(client, raw);
                case "leave" -> handleLeave(client);
                case "chat" -> handleChat(client, raw);
                case "rooms" -> handleRooms(client);
                case "users" -> handleUsers(client);
                default -> sendJson(client, "error", "content", "Unknown message type");
            }
        } catch (Exception e) {
            logger.error("Error handling WS message: {}", e.getMessage());
            sendJson(client, "error", "content", "Server error");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WebSocketChatClient client = clients.remove(conn);
        if (client == null) return;

        Room room = client.getCurrentRoom();
        if (room != null) {
            room.leave(client);
            broadcastRoomUsers(room);
        }
        clientRegistry.unregister(client.getClientId());
        logger.info("WebSocket client '{}' disconnected", client.getUsername());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            logger.error("WebSocket error for {}: {}", conn.getRemoteSocketAddress(), ex.getMessage());
        } else {
            logger.error("WebSocket server error: {}", ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        logger.info("WebSocket server started on port {}", getPort());
    }

    // ─── Command Handlers ───

    private void handleAuth(WebSocketChatClient client, String raw) {
        String username = extractJsonField(raw, "username");
        if (username == null || username.isBlank()) {
            sendJson(client, "auth_fail", "error", "Username cannot be blank");
            return;
        }
        username = username.trim();
        client.setUsername(username);
        if (!clientRegistry.register(client)) {
            client.setUsername(null);
            sendJson(client, "auth_fail", "error", "Username '" + username + "' is taken");
            return;
        }
        sendJson(client, "auth_ok", "username", username);
        broadcastRoomList();
    }

    private void handleJoin(WebSocketChatClient client, String raw) {
        if (client.getUsername() == null) {
            sendJson(client, "error", "content", "Not authenticated");
            return;
        }
        String roomName = extractJsonField(raw, "room");
        if (roomName == null || roomName.isBlank()) {
            sendJson(client, "error", "content", "Room name required");
            return;
        }

        Room prevRoom = client.getCurrentRoom();
        if (prevRoom != null) {
            prevRoom.leave(client);
            broadcastRoomUsers(prevRoom);
        }

        Room room = roomManager.getOrCreateRoom(roomName.trim());
        room.join(client);

        sendJson(client, "joined", "room", roomName);
        broadcastRoomUsers(room);
        broadcastRoomList();
    }

    private void handleLeave(WebSocketChatClient client) {
        Room room = client.getCurrentRoom();
        if (room == null) return;
        room.leave(client);
        sendJson(client, "left", "room", room.getName());
        broadcastRoomUsers(room);
        broadcastRoomList();
    }

    private void handleChat(WebSocketChatClient client, String raw) {
        if (client.getUsername() == null) return;
        Room room = client.getCurrentRoom();
        if (room == null) {
            sendJson(client, "error", "content", "Join a room first");
            return;
        }
        String content = extractJsonField(raw, "content");
        if (content == null || content.isBlank()) return;

        Message msg = new Message(client.getUsername(), content, room.getName(), MessageType.CHAT);
        room.submitMessage(msg);
    }

    private void handleRooms(WebSocketChatClient client) {
        broadcastRoomList();
    }

    private void handleUsers(WebSocketChatClient client) {
        Room room = client.getCurrentRoom();
        if (room != null) {
            broadcastRoomUsers(room);
        }
    }

    // ─── Broadcast Helpers ───

    private void broadcastRoomList() {
        Map<String, Integer> info = roomManager.getRoomInfo();
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (var entry : info.entrySet()) {
            if (!first) sb.append(",");
            sb.append("{\"name\":\"").append(escapeJson(entry.getKey()))
              .append("\",\"members\":").append(entry.getValue()).append("}");
            first = false;
        }
        sb.append("]");

        String json = "{\"type\":\"room_list\",\"rooms\":" + sb + "}";
        for (WebSocketChatClient c : clients.values()) {
            if (c.getUsername() != null) c.sendMessage(json);
        }
    }

    private void broadcastRoomUsers(Room room) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ChatClient member : room.getMembers()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(member.getUsername())).append("\"");
            first = false;
        }
        sb.append("]");

        String json = "{\"type\":\"user_list\",\"room\":\"" + escapeJson(room.getName())
                + "\",\"users\":" + sb + "}";
        for (ChatClient member : room.getMembers()) {
            member.sendMessage(json);
        }
    }

    // ─── JSON Utilities (minimal, no library needed) ───

    private void sendJson(WebSocketChatClient client, String type, String key, String value) {
        client.sendMessage("{\"type\":\"" + type + "\",\"" + key + "\":\"" + escapeJson(value) + "\"}");
    }

    static String extractJsonField(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx = json.indexOf(":", idx) + 1;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (idx >= json.length()) return null;
        if (json.charAt(idx) == '"') {
            int end = json.indexOf('"', idx + 1);
            return end > 0 ? json.substring(idx + 1, end) : null;
        }
        int end = idx;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(idx, end).trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
