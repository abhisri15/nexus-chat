# NexusChat

A concurrent multi-room chat server in Java demonstrating real-world application
of the **Producer-Consumer pattern**, thread synchronization, backpressure, and
design patterns (Observer, Strategy, Dependency Inversion).

---

## Architecture

- **Thread-per-client** model with `ExecutorService` thread pool
- **Per-room bounded message queues** with `wait()/notifyAll()` blocking
- **Fan-out broadcast** — one consumer thread per room delivers to all members
- **Backpressure** — slow clients handled via Strategy pattern
- **Observer pattern** — decoupled event logging

See `docs/` for detailed design:
- [HLD.md](docs/HLD.md) — High Level Design
- [LLD.md](docs/LLD.md) — Low Level Design (class diagrams, thread model, concurrency)
- [FLOW.md](docs/FLOW.md) — Flow diagrams (connection, messaging, shutdown)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build | Gradle |
| Networking | `java.net` blocking I/O |
| Concurrency | `synchronized`/`wait`/`notifyAll`, `ExecutorService`, `ConcurrentHashMap`, `CopyOnWriteArrayList`, `AtomicInteger` |
| Logging | SLF4J + Logback |
| Testing | JUnit 5 |

## Build & Run

**Prerequisites:** Java 17+

```bash
# Start the server
./gradlew run

# In another terminal — start a client
./gradlew runClient

# Open more terminals for more clients
./gradlew runClient
```

## Client Commands

```
/join <room>   — Join or create a room
/leave         — Leave current room
/rooms         — List all active rooms
/users         — List users in current room
/quit          — Disconnect
```

Any text not starting with `/` is sent as a chat message to your current room.

## Project Structure

```
src/main/java/com/nexuschat/
├── NexusChatServer.java              # Entry point
├── server/
│   ├── ChatServer.java               # Accept loop, thread pool, lifecycle
│   └── ServerConfig.java             # Immutable config
├── client/
│   ├── ConnectedClient.java          # Socket wrapper, thread-safe writes
│   ├── ClientRegistry.java           # ConcurrentHashMap client lookup
│   ├── ClientHandler.java            # PRODUCER — reads client, enqueues to room
│   └── NexusChatClient.java          # Standalone CLI client
├── room/
│   ├── Room.java                     # Shared resource — members + queue + broadcaster
│   └── RoomManager.java             # ConcurrentHashMap room lifecycle
├── message/
│   ├── Message.java                  # Immutable message object
│   ├── MessageType.java             # CHAT, JOIN, LEAVE, SYSTEM, BROADCAST
│   └── ChatProtocol.java            # Wire format encode/decode
├── queue/
│   ├── BoundedMessageQueue.java     # Interface (Strategy pattern)
│   └── RoomMessageQueue.java        # wait/notifyAll impl (from assignment)
├── broadcast/
│   ├── MessageBroadcaster.java      # CONSUMER — dequeues, fans out to members
│   ├── BackpressureHandler.java     # Interface (Strategy pattern)
│   ├── SlowClientAction.java        # Enum — DROP, DISCONNECT, RETRY
│   └── DropMessageHandler.java      # Default — drop message for slow client
├── observer/
│   ├── RoomEventListener.java       # Interface (Observer pattern)
│   └── ConsoleRoomLogger.java       # SLF4J logging implementation
└── exception/
    ├── ChatException.java            # Base exception
    ├── RoomFullException.java
    └── ClientDisconnectedException.java
```

## Concurrency Concepts Demonstrated

| Concept | Where |
|---|---|
| Producer-Consumer | ClientHandler → Queue → MessageBroadcaster |
| Bounded Buffer / Backpressure | RoomMessageQueue blocks when full |
| Monitor Pattern | `synchronized` + `wait()` + `notifyAll()` |
| Thread Pool | `ExecutorService` for client handlers |
| Fan-out | One message → N client writes |
| Thread-safe Collections | `ConcurrentHashMap`, `CopyOnWriteArrayList` |
| Atomic Operations | `AtomicInteger` for counters |
| Volatile | Visibility flags (`running`, `connected`) |
| Graceful Shutdown | Shutdown hook, queue drain, thread join |
