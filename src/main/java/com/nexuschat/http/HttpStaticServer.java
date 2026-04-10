package com.nexuschat.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Lightweight HTTP server that serves static files (HTML, CSS, JS)
 * from the classpath resource directory /static/.
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
    private HttpServer server;

    public HttpStaticServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRequest);
        server.setExecutor(null); // default executor
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

        // Default to index.html
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

    private String getContentType(String path) {
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            String ext = path.substring(dot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
