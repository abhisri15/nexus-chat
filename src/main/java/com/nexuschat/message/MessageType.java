package com.nexuschat.message;

/**
 * Types of messages flowing through the system.
 *
 * CHAT      — Regular user message (goes through room queue)
 * JOIN      — User joined a room (goes through room queue for ordering)
 * LEAVE     — User left a room (goes through room queue for ordering)
 * SYSTEM    — Server-to-client direct message (does NOT go through queue)
 * BROADCAST — Server-wide announcement (e.g., shutdown notice)
 */
public enum MessageType {
    CHAT,
    JOIN,
    LEAVE,
    SYSTEM,
    BROADCAST
}
