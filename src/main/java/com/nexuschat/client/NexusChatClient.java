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
        // TODO: 1. Open socket to host:port
        //       2. Create serverReader (from socket input)
        //       3. Create serverWriter (from socket output)
        //       4. Create consoleReader (from System.in)
        //       5. Set running = true
        //       6. Start reader thread (reads from server, prints to console)
        //       7. Start writer loop on main thread (reads console, sends to server)
        //       8. On exit: disconnect()
    }

    /**
     * Background thread: reads lines from server and prints to console.
     */
    private void startReaderThread() {
        // TODO: New daemon thread that loops:
        //       - line = serverReader.readLine()
        //       - if null → server disconnected, set running=false, break
        //       - System.out.println(line)
    }

    /**
     * Main thread loop: reads console input and sends to server.
     */
    private void startWriterLoop() {
        // TODO: while (running)
        //       - line = consoleReader.readLine()
        //       - if "/quit" → break
        //       - serverWriter.println(line)
        //       - serverWriter.flush()
    }

    /**
     * Close all resources.
     */
    public void disconnect() {
        // TODO: Set running = false
        //       Close socket (closes reader/writer too)
    }
}
