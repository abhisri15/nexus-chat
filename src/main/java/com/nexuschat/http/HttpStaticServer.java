package com.nexuschat.http;

import com.nexuschat.client.ClientRegistry;
import com.nexuschat.room.Room;
import com.nexuschat.room.RoomManager;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;

/**
 * Lightweight HTTP server that serves static files (HTML, CSS, JS)
 * from the classpath resource directory /static/ and exposes a
 * /stats JSON endpoint for live server metrics.
 *
 * Uses JDK's built-in com.sun.net.httpserver — no external dependency needed.
 */
public class HttpStaticServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpStaticServer.class);

    private static final Map<String, String> MIME_TYPES = Map.of(
            "html", "text/html; charset=UTF-8",
            "css", "text/css; charset=UTF-8",
            "js", "application/javascript; charset=UTF-8",
            "json", "application/json; charset=UTF-8",
            "png", "image/png",
            "svg", "image/svg+xml",
            "ico", "image/x-icon"
    );

    private final int port;
    private final RoomManager roomManager;
    private final ClientRegistry clientRegistry;
    private final Instant startTime;
    private HttpServer server;

    public HttpStaticServer(int port, RoomManager roomManager, ClientRegistry clientRegistry) {
        this.port = port;
        this.roomManager = roomManager;
        this.clientRegistry = clientRegistry;
        this.startTime = Instant.now();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRequest);
        server.setExecutor(null);
        server.start();
        logger.info("HTTP static server started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            logger.info("HTTP static server stopped");
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if ("/stats".equals(path)) {
            handleStats(exchange);
            return;
        }

        if ("/".equals(path)) {
            path = "/index.html";
        }

        String resourcePath = "/static" + path;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            byte[] bytes = is.readAllBytes();
            String contentType = getContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        long uptimeSeconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        Map<String, Room> rooms = roomManager.getAllRooms();
        int totalMessages = 0;

        StringBuilder roomDetails = new StringBuilder("[");
        boolean first = true;
        for (var entry : rooms.entrySet()) {
            Room room = entry.getValue();
            int count = room.getMessageCount();
            totalMessages += count;
            if (!first) roomDetails.append(",");
            roomDetails.append("{\"name\":\"").append(escapeJson(entry.getKey()))
                    .append("\",\"members\":").append(room.getMemberCount())
                    .append(",\"messages\":").append(count).append("}");
            first = false;
        }
        roomDetails.append("]");

        String json = "{\"uptime_seconds\":" + uptimeSeconds
                + ",\"rooms\":" + rooms.size()
                + ",\"users_online\":" + clientRegistry.getOnlineCount()
                + ",\"total_messages\":" + totalMessages
                + ",\"room_details\":" + roomDetails + "}";

        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getContentType(String path) {
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            String ext = path.substring(dot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
