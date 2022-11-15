package com.philippkutsch.tuchain.network.protocol.network;

import javax.annotation.Nonnull;

public class NetworkMessageHandlerContainer<T> {
    private final Class<T> messageClass;
    private final String type;
    private final NetworkMessageHandler<T> messageHandler;

    public NetworkMessageHandlerContainer(
            @Nonnull Class<T> messageClass,
            @Nonnull String type,
            @Nonnull NetworkMessageHandler<T> messageHandler) {
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

    public NetworkMessageHandler<T> getMessageHandler() {
        return messageHandler;
    }
}
