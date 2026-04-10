package com.nexuschat.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientRegistryTest {

    private ClientRegistry registry;

    /**
     * Creates a mock ConnectedClient without needing a real socket.
     */
    private ConnectedClient mockClient(String clientId, String username) {
        ConnectedClient client = mock(ConnectedClient.class);
        when(client.getClientId()).thenReturn(clientId);
        when(client.getUsername()).thenReturn(username);
        return client;
    }

    @BeforeEach
    void setUp() {
        registry = new ClientRegistry();
    }

    @Test
    void register_success() {
        ConnectedClient client = mockClient("c1", "alice");
        assertTrue(registry.register(client));
        assertEquals(1, registry.getOnlineCount());
    }

    @Test
    void register_duplicateUsername_rejected() {
        ConnectedClient alice1 = mockClient("c1", "alice");
        ConnectedClient alice2 = mockClient("c2", "alice");

        assertTrue(registry.register(alice1));
        assertFalse(registry.register(alice2));
        assertEquals(1, registry.getOnlineCount());
    }

    @Test
    void register_differentUsernames_bothSucceed() {
        assertTrue(registry.register(mockClient("c1", "alice")));
        assertTrue(registry.register(mockClient("c2", "bob")));
        assertEquals(2, registry.getOnlineCount());
    }

    @Test
    void lookupByUsername() {
        ConnectedClient alice = mockClient("c1", "alice");
        registry.register(alice);

        assertSame(alice, registry.getByUsername("alice"));
        assertNull(registry.getByUsername("bob"));
    }

    @Test
    void lookupByClientId() {
        ConnectedClient alice = mockClient("c1", "alice");
        registry.register(alice);

        assertSame(alice, registry.getByClientId("c1"));
        assertNull(registry.getByClientId("c99"));
    }

    @Test
    void isUsernameTaken() {
        registry.register(mockClient("c1", "alice"));

        assertTrue(registry.isUsernameTaken("alice"));
        assertFalse(registry.isUsernameTaken("bob"));
    }

    @Test
    void unregister_removesFromBothMaps() {
        ConnectedClient alice = mockClient("c1", "alice");
        registry.register(alice);
        assertEquals(1, registry.getOnlineCount());

        registry.unregister("c1");

        assertEquals(0, registry.getOnlineCount());
        assertNull(registry.getByUsername("alice"));
        assertNull(registry.getByClientId("c1"));
    }

    @Test
    void unregister_nonExistentId_noOp() {
        registry.unregister("doesNotExist");
        assertEquals(0, registry.getOnlineCount());
    }

    @Test
    void unregister_freesUsername_forReuse() {
        ConnectedClient alice1 = mockClient("c1", "alice");
        registry.register(alice1);
        registry.unregister("c1");

        ConnectedClient alice2 = mockClient("c2", "alice");
        assertTrue(registry.register(alice2));
    }

    @Test
    void getAllClients_returnsAll() {
        registry.register(mockClient("c1", "alice"));
        registry.register(mockClient("c2", "bob"));

        assertEquals(2, registry.getAllClients().size());
    }
}
