package com.nexuschat.room;

import com.nexuschat.observer.RoomEventListener;
import com.nexuschat.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe lifecycle manager for chat rooms.
 *
 * Uses ConcurrentHashMap — rooms can be created and destroyed from
 * multiple ClientHandler threads simultaneously.
 *
 * Rooms are created lazily on first /join and optionally destroyed
 * when the last member leaves.
 */
public class RoomManager {

    private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ServerConfig config;
    private final RoomEventListener eventListener;

    public RoomManager(ServerConfig config, RoomEventListener eventListener) {
        this.config = config;
        this.eventListener = eventListener;
    }

    /**
     * Get an existing room or create a new one.
     * Thread-safe via ConcurrentHashMap.computeIfAbsent.
     */
    public Room getOrCreateRoom(String name) {
        // TODO: Use computeIfAbsent to atomically create room if missing
        //       If newly created: start broadcaster, notify observer
        //       Return the room
        return null;
    }

    /**
     * Get a room by name. Returns null if not found.
     */
    public Room getRoom(String name) {
        return rooms.get(name);
    }

    /**
     * Remove and destroy a room (when empty).
     */
    public void removeRoom(String name) {
        // TODO: Remove from map
        //       Stop broadcaster
        //       Notify observer: onRoomDestroyed
    }

    /**
     * List all room names.
     */
    public Set<String> listRoomNames() {
        return rooms.keySet();
    }

    /**
     * Get room info: name → member count.
     */
    public Map<String, Integer> getRoomInfo() {
        return rooms.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getMemberCount()));
    }

    /**
     * Shutdown all rooms. Called during server shutdown.
     */
    public void shutdownAllRooms() {
        // TODO: Iterate all rooms, stop each broadcaster, clear the map
    }
}
