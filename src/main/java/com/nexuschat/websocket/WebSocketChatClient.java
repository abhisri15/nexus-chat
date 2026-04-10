package com.nexuschat.websocket;

import com.nexuschat.client.ChatClient;
import com.nexuschat.room.Room;
import org.java_websocket.WebSocket;

import java.time.Instant;
import java.util.UUID;

/**
 * ChatClient implementation backed by a WebSocket connection.
 *
 * Allows browser-based clients to participate in the same rooms
 * as TCP clients — same interface, different transport.
 *
 * sendMessage converts protocol-formatted plain text (from MessageBroadcaster)
 * into JSON for the browser. Messages already in JSON (from WebSocketChatServer)
 * are passed through unchanged.
 */
public class WebSocketChatClient implements ChatClient {

    private final String clientId;
    private final WebSocket webSocket;
    private String username;
    private Room currentRoom;
    private volatile boolean connected;

    public WebSocketChatClient(WebSocket webSocket) {
        this.clientId = UUID.randomUUID().toString().substring(0, 8);
        this.webSocket = webSocket;
        this.connected = true;
    }

    @Override
    public void sendMessage(String rawMessage) {
        if (!connected || !webSocket.isOpen()) return;
        try {
            String json = rawMessage.startsWith("{") ? rawMessage : protocolToJson(rawMessage);
            webSocket.send(json);
        } catch (Exception ignored) {
        }
    }

    /**
     * Convert ChatProtocol.formatForDisplay() output into browser-friendly JSON.
     *
     * Input formats:
     *   [#room] sender: content          → {"type":"chat", ...}
     *   [#room] >> sender joined the room → {"type":"system", ...}
     *   [#room] << sender left the room   → {"type":"system", ...}
     *   [SERVER] text                     → {"type":"system", ...}
     *   [BROADCAST] text                  → {"type":"system", ...}
     */
    private String protocolToJson(String formatted) {
        if (formatted.startsWith("[#")) {
            int close = formatted.indexOf(']');
            if (close < 0) return systemJson(formatted);
            String rest = formatted.substring(close + 2);

            if (rest.startsWith(">> ")) {
                return systemJson(rest.substring(3));
            }
            if (rest.startsWith("<< ")) {
                return systemJson(rest.substring(3));
            }
            int colon = rest.indexOf(": ");
            if (colon > 0) {
                String sender = rest.substring(0, colon);
                String content = rest.substring(colon + 2);
                return "{\"type\":\"chat\",\"sender\":\"" + esc(sender)
                        + "\",\"content\":\"" + esc(content)
                        + "\",\"timestamp\":\"" + Instant.now() + "\"}";
            }
        }
        if (formatted.startsWith("[SERVER] ")) {
            return systemJson(formatted.substring(9));
        }
        if (formatted.startsWith("[BROADCAST] ")) {
            return systemJson(formatted.substring(12));
        }
        return systemJson(formatted);
    }

    private static String systemJson(String content) {
        return "{\"type\":\"system\",\"content\":\"" + esc(content) + "\"}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void disconnect() {
        connected = false;
        if (webSocket.isOpen()) {
            webSocket.close();
        }
    }

    @Override
    public String getClientId() { return clientId; }

    @Override
    public String getUsername() { return username; }

    @Override
    public void setUsername(String username) { this.username = username; }

    @Override
    public Room getCurrentRoom() { return currentRoom; }

    @Override
    public void setCurrentRoom(Room room) { this.currentRoom = room; }

    @Override
    public boolean isConnected() { return connected && webSocket.isOpen(); }

    public WebSocket getWebSocket() { return webSocket; }
}
