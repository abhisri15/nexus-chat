package com.nexuschat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Standalone CLI chat client.
 *
 * Connects to the NexusChat server via TCP socket.
 * Two threads:
 *   - Reader thread: continuously reads from server socket, prints to console
 *   - Main thread: reads from System.in, sends to server
 *
 * Usage:
 *   ./gradlew runClient
 *   (or) java -cp build/classes/java/main com.nexuschat.client.NexusChatClient [host] [port]
 */
public class NexusChatClient {

    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;
    private BufferedReader consoleReader;
    private volatile boolean running;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

        NexusChatClient client = new NexusChatClient();
        client.connect(host, port);
    }

    /**
     * Connect to the server and start reading/writing.
     */
    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverWriter = new PrintWriter(socket.getOutputStream(), true);
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            running = true;

            System.out.println("Connected to NexusChat at " + host + ":" + port);
            startReaderThread();
            startWriterLoop();
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Background thread: reads lines from server and prints to console.
     */
    private void startReaderThread() {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = serverReader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Disconnected from server.");
                }
            }
            running = false;
        }, "reader-thread");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Main thread loop: reads console input and sends to server.
     */
    private void startWriterLoop() {
        try {
            while (running) {
                String line = consoleReader.readLine();
                if (line == null || line.equalsIgnoreCase("/quit")) {
                    serverWriter.println("/quit");
                    serverWriter.flush();
                    break;
                }
                serverWriter.println(line);
                serverWriter.flush();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error reading input: " + e.getMessage());
            }
        }
    }

    /**
     * Close all resources.
     */
    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
