package com.nexuschat.room;

import com.nexuschat.client.ConnectedClient;
import com.nexuschat.message.Message;
import com.nexuschat.message.MessageType;
import com.nexuschat.observer.RoomEventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomTest {

    private Room room;
    private RoomEventListener listener;

    private ConnectedClient mockClient(String username) {
        ConnectedClient client = mock(ConnectedClient.class);
        when(client.getUsername()).thenReturn(username);
        when(client.isConnected()).thenReturn(true);
        return client;
    }

    @BeforeEach
    void setUp() {
        listener = mock(RoomEventListener.class);
        room = new Room("general", 10, listener);
        room.startBroadcaster();
    }

    @AfterEach
    void tearDown() {
        room.stopBroadcaster();
    }

    @Test
    void join_addsMemberAndNotifiesObserver() {
        ConnectedClient alice = mockClient("alice");
        room.join(alice);

        assertEquals(1, room.getMemberCount());
        assertTrue(room.getMembers().contains(alice));
        verify(alice).setCurrentRoom(room);
        verify(listener).onClientJoined(alice, room);
    }

    @Test
    void leave_removesMemberAndNotifiesObserver() {
        ConnectedClient alice = mockClient("alice");
        room.join(alice);
        room.leave(alice);

        assertEquals(0, room.getMemberCount());
        verify(alice).setCurrentRoom(null);
        verify(listener).onClientLeft(alice, room);
    }

    @Test
    void leave_clientNotInRoom_noOp() {
        ConnectedClient alice = mockClient("alice");
        // Never joined — should not throw or notify
        room.leave(alice);

        assertEquals(0, room.getMemberCount());
        verify(listener, never()).onClientLeft(any(), any());
    }

    @Test
    void leave_doubleLeave_idempotent() {
        ConnectedClient alice = mockClient("alice");
        room.join(alice);
        room.leave(alice);
        room.leave(alice);

        assertEquals(0, room.getMemberCount());
        // onClientLeft called exactly once
        verify(listener, times(1)).onClientLeft(alice, room);
    }

    @Test
    void submitMessage_incrementsMessageCount() throws InterruptedException {
        room.submitMessage(new Message("alice", "hello", "general", MessageType.CHAT));
        room.submitMessage(new Message("bob", "world", "general", MessageType.CHAT));

        // Wait briefly for messages to be processed by broadcaster
        Thread.sleep(200);
        assertEquals(2, room.getMessageCount());
    }

    @Test
    void join_setsRoomOnClient() {
        ConnectedClient alice = mockClient("alice");
        room.join(alice);
        verify(alice).setCurrentRoom(room);
    }

    @Test
    void multipleMembers_allTracked() {
        ConnectedClient alice = mockClient("alice");
        ConnectedClient bob = mockClient("bob");
        ConnectedClient charlie = mockClient("charlie");

        room.join(alice);
        room.join(bob);
        room.join(charlie);

        assertEquals(3, room.getMemberCount());
        room.leave(bob);
        assertEquals(2, room.getMemberCount());
        assertFalse(room.getMembers().contains(bob));
    }

    @Test
    void stopBroadcaster_preventsNewMessages() {
        room.stopBroadcaster();
        int countBefore = room.getMessageCount();

        room.submitMessage(new Message("alice", "test", "general", MessageType.CHAT));
        // Should not increment — active=false, enqueue returns early
        assertEquals(countBefore, room.getMessageCount());
    }

    @Test
    void joinAfterStop_noOp() {
        room.stopBroadcaster();
        ConnectedClient alice = mockClient("alice");
        room.join(alice);

        assertEquals(0, room.getMemberCount());
        verify(listener, never()).onClientJoined(any(), any());
    }

    @Test
    void name_returnsCorrectly() {
        assertEquals("general", room.getName());
    }

    @Test
    void queueStatus_formatsCorrectly() {
        assertTrue(room.getQueueStatus().startsWith("Queue["));
    }
}
