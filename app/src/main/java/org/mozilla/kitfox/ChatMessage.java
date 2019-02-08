package org.mozilla.kitfox;

/**
 * Chat Message
 *
 * Represents a message in a chat.
 */
public class ChatMessage {

    public boolean direction; // true is incoming, false is outgoing
    public String messageText;

    /**
     * Chat Message constructor
     *
     * @param direction Incoming (true) or outgoing (false)
     * @param messageText The text of the message sent/received
     */
    public ChatMessage(boolean direction, String messageText) {
        super();
        this.direction = direction;
        this.messageText = messageText;
    }
}
