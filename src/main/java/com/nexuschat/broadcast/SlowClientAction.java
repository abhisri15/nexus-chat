package com.nexuschat.broadcast;

/**
 * Actions the broadcaster can take when a client can't keep up.
 */
public enum SlowClientAction {
    /** Skip this message for this client, continue to next member */
    DROP_MESSAGE,

    /** Disconnect the slow client entirely */
    DISCONNECT_CLIENT,

    /** Try delivering one more time before giving up */
    RETRY_ONCE
}
