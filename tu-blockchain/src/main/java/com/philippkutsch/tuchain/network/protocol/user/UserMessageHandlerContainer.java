package com.philippkutsch.tuchain.network.protocol.user;

import javax.annotation.Nonnull;

public class UserMessageHandlerContainer<T> {
    private final Class<T> messageClass;
    private final String type;
    private final UserMessageHandler<T> messageHandler;

    public UserMessageHandlerContainer(
            @Nonnull Class<T> messageClass,
            @Nonnull String type,
            @Nonnull UserMessageHandler<T> messageHandler) {
        this.messageClass = messageClass;
        this.type = type;
        this.messageHandler = messageHandler;
    }

    public Class<T> getMessageClass() {
        return messageClass;
    }

    public String getType() {
        return type;
    }

    public UserMessageHandler<T> getMessageHandler() {
        return messageHandler;
    }
}
