# NexusChat - Flow Diagrams

---

## 1. Server Startup Flow

```mermaid
sequenceDiagram
    participant Main as main()
    participant Cfg as ServerConfig
    participant CS as ChatServer
    participant RM as RoomManager
    participant CR as ClientRegistry
    participant LOG as ConsoleRoomLogger
    participant JVM as JVM Shutdown Hook

    Main->>Cfg: new ServerConfig(9090, 200, 50, 100, 30000)
    Main->>LOG: new ConsoleRoomLogger()
    Main->>CS: new ChatServer(config, eventListener)
    CS->>RM: new RoomManager(config, eventListener)
    CS->>CR: new ClientRegistry()
    CS->>CS: threadPool = Executors.newFixedThreadPool(config.threadPoolSize)
    Main->>JVM: Runtime.addShutdownHook → CS.stop()
    Main->>CS: start()
    CS->>CS: serverSocket = new ServerSocket(port)
    CS->>CS: log "NexusChat started on port 9090"

    loop Accept Loop (while running)
        CS->>CS: socket = serverSocket.accept()
        CS->>CS: client = new ConnectedClient(socket)
        CS->>CS: handler = new ClientHandler(client, roomManager, registry, eventListener)
        CS->>CS: threadPool.submit(handler)
    end
```

---

## 2. Client Connection & Registration Flow

```mermaid
sequenceDiagram
    participant CLI as NexusChatClient
    participant SS as ServerSocket
    participant CH as ClientHandler
    participant CC as ConnectedClient
    participant CR as ClientRegistry

    CLI->>SS: connect(host, port)
    SS->>CH: accept() → new ClientHandler

    CH->>CC: sendMessage("Welcome to NexusChat! Enter username:")
    CLI->>CC: readLine() → "abhikalp"
    CH->>CR: isUsernameTaken("abhikalp")

    alt Username taken
        CH->>CC: sendMessage("Username taken. Try another:")
        CLI->>CC: readLine() → "abhikalp_2"
    end

    CH->>CC: setUsername("abhikalp")
    CH->>CR: register(client)
    CH->>CC: sendMessage("Hello abhikalp! Type /join <room> to start chatting.")

    loop Message Read Loop
        CH->>CC: readLine()
        Note over CH: Parse and route...
    end
```

---

## 3. Join Room Flow

```mermaid
sequenceDiagram
    participant CH as ClientHandler (Producer)
    participant CC as ConnectedClient
    participant RM as RoomManager
    participant R as Room
    participant Q as BoundedMessageQueue
    participant B as MessageBroadcaster
    participant OBS as RoomEventListener

    CH->>CH: handleJoin("general")

    alt Client already in a room
        CH->>CH: handleLeave() first
    end

    CH->>RM: getOrCreateRoom("general")

    alt Room doesn't exist
        RM->>R: new Room("general", config.queueCapacity, eventListener)
        R->>Q: new RoomMessageQueue(capacity=50)
        R->>B: new MessageBroadcaster(queue, room, backpressureHandler, eventListener)
        R->>B: start broadcaster thread
        RM->>OBS: onRoomCreated(room)
        RM-->>CH: return room
    else Room exists
        RM-->>CH: return existing room
    end

    CH->>R: join(client)
    R->>R: members.add(client)
    R->>CC: setCurrentRoom(room)
    R->>OBS: onClientJoined(client, room)
    R->>R: submitMessage(JOIN message: "abhikalp joined #general")

    Note over Q,B: JOIN message flows through the queue<br/>like any other message — consistent ordering
```

---

## 4. Chat Message Flow (Core Producer-Consumer)

```mermaid
sequenceDiagram
    participant CLI as Client Terminal
    participant CC as ConnectedClient
    participant CH as ClientHandler (PRODUCER)
    participant Q as RoomMessageQueue
    participant MON as Queue Monitor
    participant B as MessageBroadcaster (CONSUMER)
    participant M1 as Member 1
    participant M2 as Member 2
    participant M3 as Member 3
    participant OBS as RoomEventListener

    CLI->>CC: "Hello everyone!"
    CC->>CH: readLine() returns "Hello everyone!"
    CH->>CH: isCommand("Hello everyone!") → false
    CH->>CH: handleChatMessage("Hello everyone!")
    CH->>CH: msg = new Message("abhikalp", "Hello everyone!", "general", CHAT)

    CH->>Q: enqueue(msg, callback)

    rect rgb(255, 235, 235)
        Note over Q,MON: synchronized(monitor)
        alt Queue is full (size >= capacity)
            Q->>MON: monitor.wait()
            Note over CH: PRODUCER BLOCKS HERE<br/>Backpressure!
            B->>Q: dequeue() removes a message
            Q->>MON: monitor.notifyAll()
            Note over CH: PRODUCER UNBLOCKS
        end
        Q->>Q: queue.addLast(msg)
        Q->>CH: callback(msg, queueSize)
        Q->>MON: monitor.notifyAll()
        Note over B: CONSUMER WAKES UP
    end

    B->>Q: dequeue(callback)
    rect rgb(235, 255, 235)
        Note over Q,MON: synchronized(monitor)
        Q->>Q: msg = queue.pollFirst()
        Q->>B: callback(msg, queueSize)
        Q->>MON: monitor.notifyAll()
        Note over CH: PRODUCER WAKES UP<br/>(if it was blocked)
    end

    B->>OBS: onMessageBroadcast(msg, room)
    B->>B: formatted = ChatProtocol.formatForDisplay(msg)

    par Fan-out to all members
        B->>M1: sendMessage(formatted)
    and
        B->>M2: sendMessage(formatted)
    and
        B->>M3: sendMessage(formatted)
    end

    Note over M1,M3: If any sendMessage() fails →<br/>BackpressureHandler decides action
```

---

## 5. Leave Room Flow

```mermaid
sequenceDiagram
    participant CH as ClientHandler
    participant CC as ConnectedClient
    participant R as Room
    participant OBS as RoomEventListener
    participant RM as RoomManager

    CH->>CH: handleLeave()
    CH->>CC: getCurrentRoom() → room

    alt Not in any room
        CH->>CC: sendMessage("You're not in any room.")
    else In a room
        CH->>R: leave(client)
        R->>R: members.remove(client)
        R->>CC: setCurrentRoom(null)
        R->>OBS: onClientLeft(client, room)
        R->>R: submitMessage(LEAVE: "abhikalp left #general")

        alt Room is now empty
            R->>R: stopBroadcaster()
            CH->>RM: removeRoom("general")
            RM->>OBS: onRoomDestroyed(room)
        end

        CH->>CC: sendMessage("Left #general")
    end
```

---

## 6. Client Disconnect Flow (Abrupt)

```mermaid
sequenceDiagram
    participant CLI as Client Terminal
    participant CC as ConnectedClient
    participant CH as ClientHandler
    participant R as Room
    participant CR as ClientRegistry
    participant OBS as RoomEventListener

    CLI->>CLI: Ctrl+C / network drop

    CH->>CC: readLine()
    CC-->>CH: IOException or null

    CH->>CH: handleDisconnect()

    alt Client was in a room
        CH->>R: leave(client)
        R->>OBS: onClientLeft(client, room)
        R->>R: submitMessage(LEAVE message)
    end

    CH->>CR: unregister(client.getClientId())
    CH->>CC: disconnect()
    CC->>CC: socket.close()
    CH->>OBS: onError("ClientHandler", disconnectException)

    Note over CH: Thread returns to pool<br/>(ClientHandler.run() exits)
```

---

## 7. Graceful Server Shutdown Flow

```mermaid
sequenceDiagram
    participant JVM as Shutdown Hook
    participant CS as ChatServer
    participant CR as ClientRegistry
    participant RM as RoomManager
    participant R as Room
    participant Q as RoomMessageQueue
    participant B as MessageBroadcaster
    participant CC as ConnectedClient

    JVM->>CS: stop()
    CS->>CS: running = false

    CS->>CR: getAllClients()
    loop For each client
        CS->>CC: sendMessage("Server shutting down...")
    end

    CS->>RM: shutdownAllRooms()
    loop For each room
        RM->>R: stopBroadcaster()
        R->>B: stop()
        B->>B: running = false
        R->>Q: shutdown()
        Q->>Q: active = false
        Q->>Q: monitor.notifyAll()
        Note over B: Consumer unblocks from<br/>wait(), sees running=false, exits
    end

    CS->>CS: serverSocket.close()
    Note over CS: Accept loop breaks<br/>with SocketException

    CS->>CS: threadPool.shutdownNow()
    Note over CS: All ClientHandler threads<br/>get interrupted

    loop For each client
        CS->>CC: disconnect()
        CC->>CC: socket.close()
    end

    CS->>CS: threadPool.awaitTermination(10, SECONDS)
    CS->>CS: log "NexusChat shut down."
```

---

## 8. Backpressure Flow (Slow Client)

```mermaid
sequenceDiagram
    participant B as MessageBroadcaster
    participant CC as Slow Client
    participant BPH as BackpressureHandler
    participant OBS as RoomEventListener

    B->>CC: sendMessage(formatted)
    CC-->>B: IOException (write buffer full / timeout)

    B->>BPH: handleSlowClient(client, message)

    alt DropMessageHandler
        BPH-->>B: DROP_MESSAGE
        B->>OBS: onError("Broadcaster", "Dropped msg for slow client X")
        Note over B: Skip this client,<br/>continue to next member
    else DisconnectClientHandler (future)
        BPH-->>B: DISCONNECT_CLIENT
        B->>CC: disconnect()
        B->>OBS: onClientLeft(client, room)
    end

    Note over B: Continue broadcasting<br/>to remaining members
```
