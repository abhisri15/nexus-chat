# NexusChat

A concurrent multi-room chat server built in Java, demonstrating real-world application
of the **Producer-Consumer pattern**, thread synchronization, backpressure handling, and
design patterns (Observer, Strategy, Dependency Inversion).

Comes with a **dark-themed web UI** served over WebSocket and a **CLI client** over raw TCP —
both share the same rooms and can chat with each other in real time.

---

## Features

- **Multi-room chat** — create and join unlimited rooms, switch freely
- **Web UI** — modern dark-themed interface with login, room sidebar, live user list, and toast notifications
- **CLI client** — lightweight terminal client for power users
- **Cross-transport messaging** — browser and terminal users share the same rooms seamlessly
- **Message history** — last 50 messages per room stored in a thread-safe circular buffer; displayed on join
- **Typing indicators** — real-time "user is typing..." with debounced emission and animated dots
- **Auto-reconnect** — exponential backoff (1s → 30s cap) with automatic re-auth and room rejoin
- **Live metrics** — `GET /stats` JSON endpoint with uptime, rooms, users, per-room message counts
- **CI pipeline** — GitHub Actions builds and tests on every push
- **Bounded message queues** — per-room `wait()/notifyAll()` blocking with configurable capacity
- **Backpressure handling** — slow clients managed via Strategy pattern (drop / disconnect / retry)
- **Observer pattern** — decoupled event logging for room lifecycle and message flow
- **Graceful shutdown** — JVM shutdown hook drains queues, notifies clients, joins threads

---

## Architecture

```
┌───────────────────────────────────────────────────────────┐
│                    NexusChatServer                        │
│                  (entry point / wiring)                   │
├──────────┬───────────────────┬────────────────────────────┤
│  HTTP    │   WebSocket       │     TCP                    │
│  :8080   │   :9091           │     :9090                  │
│  Static  │   WebSocketChat   │     ChatServer             │
│  Server  │   Server          │     (accept loop)          │
└──────────┴────────┬──────────┴────────┬───────────────────┘
                    │   shared state    │
              ┌─────▼──────────────────▼───────┐
              │   ClientRegistry (usernames)   │
              │   RoomManager (room lifecycle) │
              └───────────────┬────────────────┘
                              │
              ┌───────────────▼─────────────────┐
              │           Room                  │
              │  ┌───────────────────────────┐  │
              │  │ BoundedMessageQueue       │  │
              │  │ (wait/notifyAll monitor)  │  │
              │  └────┬────────────────▲─────┘  │
              │       │ dequeue        │ enqueue│
              │  ┌────▼──────┐  ┌──────┴──────┐ │
              │  │Broadcaster│  │ClientHandler│ │
              │  │(CONSUMER) │  │ (PRODUCER)  │ │
              │  └────┬──────┘  └─────────────┘ │
              │       │ fan-out to members      │
              │  ┌────▼──────────────────────┐  │
              │  │ CopyOnWriteArrayList      │  │
              │  │ <ChatClient> members      │  │
              │  └───────────────────────────┘  │
              └─────────────────────────────────┘
```

- **Producers** — `ClientHandler` (TCP) and `WebSocketChatServer` (browser) read user input and enqueue messages into the room's bounded queue.
- **Consumer** — one `MessageBroadcaster` thread per room dequeues messages and fans out to all members.
- **Shared resource** — `BoundedMessageQueue` uses `synchronized` + `wait()` + `notifyAll()` for thread-safe blocking.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build | Gradle 8.7 |
| TCP Networking | `java.net` blocking I/O |
| WebSocket | [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) 1.5.4 |
| HTTP | `com.sun.net.httpserver` (JDK built-in) |
| Frontend | Vanilla HTML / CSS / JS (no frameworks) |
| Concurrency | `synchronized`/`wait`/`notifyAll`, `ExecutorService`, `ConcurrentHashMap`, `CopyOnWriteArrayList`, `AtomicInteger`, `volatile` |
| Logging | SLF4J + Logback (console + rolling file) |
| Testing | JUnit 5 + Mockito |

---

## Getting Started

### Prerequisites

- **Java 17+** (tested with Eclipse Temurin 17)
- **Gradle 8.7** (wrapper included — no manual install needed)

### Build

```bash
./gradlew build -x test
```

On Windows, if `JAVA_HOME` is not set:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
.\gradlew.bat build -x test
```

### Run

```bash
# Start the server (HTTP + WebSocket + TCP)
./gradlew run
```

This starts three subsystems:

| Service | Port | Purpose |
|---|---|---|
| HTTP Server | `8080` | Serves the web UI (`index.html`, `style.css`, `app.js`) |
| WebSocket Server | `9091` | Real-time browser ↔ server messaging |
| TCP Server | `9090` | CLI client connections |

### Use the Web UI

Open **http://localhost:8080** in your browser.

1. Enter a username on the login screen
2. Click **+** in the sidebar to create or join a room
3. Start chatting — messages appear in real time
4. Open multiple tabs to simulate multiple users

### Use the CLI Client

```bash
# In a separate terminal
./gradlew runClient
```

CLI commands:

```
/join <room>   — Join or create a room
/leave         — Leave current room
/rooms         — List all active rooms
/users         — List users in current room
/quit          — Disconnect
```

Any text not starting with `/` is sent as a chat message to your current room.

### Cross-Transport Chat

Browser users and CLI users share the same `RoomManager` and `ClientRegistry`.
A message sent from the web UI appears in the terminal client and vice versa.

### Server Metrics

Visit **http://localhost:8080/stats** for live JSON metrics:

```json
{
  "uptime_seconds": 3412,
  "rooms": 3,
  "users_online": 7,
  "total_messages": 142,
  "room_details": [
    {"name": "general", "members": 4, "messages": 89},
    {"name": "dev", "members": 3, "messages": 53}
  ]
}
```

---

## Project Structure

```
src/main/java/com/nexuschat/
├── NexusChatServer.java              # Entry point — wires HTTP + WS + TCP
├── server/
│   ├── ChatServer.java               # TCP accept loop, thread pool, lifecycle
│   └── ServerConfig.java             # Immutable config (ports, pool size, timeouts)
├── http/
│   └── HttpStaticServer.java         # JDK HttpServer — serves /static/ + /stats endpoint
├── websocket/
│   ├── WebSocketChatServer.java      # WS server — auth, join, chat, room/user lists
│   └── WebSocketChatClient.java      # ChatClient over WebSocket (protocol → JSON)
├── client/
│   ├── ChatClient.java               # Interface — transport-agnostic client contract
│   ├── ConnectedClient.java          # ChatClient over TCP socket
│   ├── ClientHandler.java            # PRODUCER — reads TCP input, enqueues messages
│   ├── ClientRegistry.java           # ConcurrentHashMap username ↔ client lookup
│   └── NexusChatClient.java          # Standalone CLI client
├── room/
│   ├── Room.java                     # Shared resource — members + queue + broadcaster + history
│   ├── RoomManager.java              # ConcurrentHashMap room lifecycle
│   └── MessageHistory.java           # Thread-safe circular buffer (last N messages)
├── message/
│   ├── Message.java                  # Immutable message (UUID, sender, content, timestamp)
│   ├── MessageType.java              # CHAT, JOIN, LEAVE, SYSTEM, BROADCAST
│   └── ChatProtocol.java             # Wire format encode/decode + display formatting
├── queue/
│   ├── BoundedMessageQueue.java      # Interface (Strategy pattern)
│   └── RoomMessageQueue.java         # synchronized + wait/notifyAll implementation
├── broadcast/
│   ├── MessageBroadcaster.java       # CONSUMER — dequeues, fans out to room members
│   ├── BackpressureHandler.java      # Interface (Strategy pattern)
│   ├── SlowClientAction.java         # Enum — DROP_MESSAGE, DISCONNECT_CLIENT, RETRY_ONCE
│   └── DropMessageHandler.java       # Default handler — logs + drops for slow clients
├── observer/
│   ├── RoomEventListener.java        # Interface — 6 lifecycle callbacks (Observer pattern)
│   └── ConsoleRoomLogger.java        # SLF4J implementation of RoomEventListener
└── exception/
    ├── ChatException.java             # Base exception
    ├── RoomFullException.java
    └── ClientDisconnectedException.java

src/main/resources/
├── logback.xml                        # Console + rolling file + error file appenders
└── static/
    ├── index.html                     # Login screen + chat layout + room modal
    ├── style.css                      # Dark theme, animations, responsive design
    └── app.js                         # WebSocket client, reconnect, typing, state machine

.github/workflows/
└── ci.yml                             # GitHub Actions — build + test on push
```

---

## Concurrency Concepts Demonstrated

| Concept | Where | How |
|---|---|---|
| **Producer-Consumer** | ClientHandler → Queue → MessageBroadcaster | Clients produce messages; one broadcaster per room consumes and delivers |
| **Bounded Buffer** | `RoomMessageQueue` | Blocks producers when queue is full, blocks consumer when empty |
| **Monitor Pattern** | `RoomMessageQueue` | `synchronized` block + `wait()` + `notifyAll()` on a shared monitor object |
| **Thread Pool** | `ChatServer` | `ExecutorService.newFixedThreadPool()` manages client handler threads |
| **Fan-out** | `MessageBroadcaster` | One dequeued message → N writes (one per room member) |
| **Thread-safe Collections** | `ClientRegistry`, `RoomManager` | `ConcurrentHashMap` with `putIfAbsent` / `computeIfAbsent` for atomic operations |
| **Copy-on-Write** | `Room.members` | `CopyOnWriteArrayList` — safe iteration during broadcast while other threads join/leave |
| **Atomic Operations** | `Room.messageCount` | `AtomicInteger` — lock-free counter incremented from producer threads |
| **Volatile Flags** | `ConnectedClient`, `MessageBroadcaster`, `ChatServer` | `volatile boolean` ensures visibility of shutdown/connected flags across threads |
| **Circular Buffer** | `MessageHistory` | `synchronized` ring buffer — broadcaster writes, joiners read concurrently |
| **Graceful Shutdown** | `NexusChatServer` | JVM shutdown hook → stop accepting → notify clients → drain queues → join broadcaster threads → close sockets |

---

## Design Patterns

| Pattern | Implementation |
|---|---|
| **Strategy** | `BackpressureHandler` interface + `DropMessageHandler` — swappable slow-client policy |
| **Strategy** | `BoundedMessageQueue` interface + `RoomMessageQueue` — swappable queue implementation |
| **Observer** | `RoomEventListener` interface + `ConsoleRoomLogger` — decoupled event logging |
| **Dependency Inversion** | `ChatClient` interface — `Room`, `MessageBroadcaster`, and `ClientRegistry` depend on abstraction, not concrete TCP/WS implementations |

---

## Testing

```bash
./gradlew test --rerun-tasks
```

| Test | What it covers |
|---|---|
| `RoomMessageQueueTest` | Enqueue/dequeue ordering, blocking behavior, shutdown wakeup |
| `ChatProtocolTest` | Encode/decode roundtrip, display formatting, command parsing |
| `ClientRegistryTest` | Register, duplicate username rejection, unregister cleanup |
| `RoomTest` | Join/leave member management, message submission |

---

## Configuration

All defaults are in `ServerConfig.java`:

| Setting | Default | Description |
|---|---|---|
| `port` | `9090` | TCP server port |
| `maxClients` | `200` | Maximum concurrent connections |
| `roomQueueCapacity` | `50` | Bounded queue size per room |
| `threadPoolSize` | `100` | Client handler thread pool |
| `clientTimeoutMs` | `30000` | Idle client timeout (ms) |

WebSocket port (`9091`) and HTTP port (`8080`) are configured in `NexusChatServer.java`.

---

## License

This project was built as part of coursework at IIIT Bhubaneswar.
