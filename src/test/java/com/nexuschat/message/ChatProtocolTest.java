package com.nexuschat.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatProtocolTest {

    @Test
    void encodeDeocde_roundtrip() {
        Message original = new Message("alice", "Hello world!", "general", MessageType.CHAT);
        String encoded = ChatProtocol.encode(original);
        Message decoded = ChatProtocol.decode(encoded);

        assertNotNull(decoded);
        assertEquals("alice", decoded.getSender());
        assertEquals("Hello world!", decoded.getContent());
        assertEquals("general", decoded.getRoomName());
        assertEquals(MessageType.CHAT, decoded.getType());
    }

    @Test
    void encode_format() {
        Message msg = new Message("bob", "hi", "random", MessageType.CHAT);
        String encoded = ChatProtocol.encode(msg);
        assertEquals("CHAT|bob|random|hi", encoded);
    }

    @Test
    void decode_nullAndBlank_returnsNull() {
        assertNull(ChatProtocol.decode(null));
        assertNull(ChatProtocol.decode(""));
        assertNull(ChatProtocol.decode("   "));
    }

    @Test
    void decode_tooFewParts_returnsNull() {
        assertNull(ChatProtocol.decode("CHAT|alice|general"));
    }

    @Test
    void decode_invalidType_returnsNull() {
        assertNull(ChatProtocol.decode("INVALID|alice|general|hello"));
    }

    @Test
    void decode_contentWithPipes_preservedCorrectly() {
        Message decoded = ChatProtocol.decode("CHAT|alice|general|hello|world|pipes");
        assertNotNull(decoded);
        assertEquals("hello|world|pipes", decoded.getContent());
    }

    @Test
    void formatForDisplay_chatMessage() {
        Message msg = new Message("alice", "Hello!", "general", MessageType.CHAT);
        assertEquals("[#general] alice: Hello!", ChatProtocol.formatForDisplay(msg));
    }

    @Test
    void formatForDisplay_joinMessage() {
        Message msg = new Message("bob", "", "general", MessageType.JOIN);
        assertEquals("[#general] >> bob joined the room", ChatProtocol.formatForDisplay(msg));
    }

    @Test
    void formatForDisplay_leaveMessage() {
        Message msg = new Message("bob", "", "general", MessageType.LEAVE);
        assertEquals("[#general] << bob left the room", ChatProtocol.formatForDisplay(msg));
    }

    @Test
    void formatForDisplay_systemMessage() {
        Message msg = new Message("server", "Welcome!", "", MessageType.SYSTEM);
        assertEquals("[SERVER] Welcome!", ChatProtocol.formatForDisplay(msg));
    }

    @Test
    void formatSystemMessage() {
        assertEquals("[SERVER] Hello", ChatProtocol.formatSystemMessage("Hello"));
    }

    @Test
    void isCommand_variousInputs() {
        assertTrue(ChatProtocol.isCommand("/join general"));
        assertTrue(ChatProtocol.isCommand("/quit"));
        assertFalse(ChatProtocol.isCommand("hello"));
        assertFalse(ChatProtocol.isCommand(""));
        assertFalse(ChatProtocol.isCommand(null));
    }

    @Test
    void parseCommand_withArgs() {
        String[] parts = ChatProtocol.parseCommand("/join general");
        assertEquals(2, parts.length);
        assertEquals("join", parts[0]);
        assertEquals("general", parts[1]);
    }

    @Test
    void parseCommand_noArgs() {
        String[] parts = ChatProtocol.parseCommand("/quit");
        assertEquals(1, parts.length);
        assertEquals("quit", parts[0]);
    }
}
