package com.philippkutsch.tuchain.network.protocol.network.messages;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.philippkutsch.tuchain.network.protocol.Message;

import javax.annotation.Nonnull;

public class PingMessage {
    public static final String TYPE = "ping";

    private final long timestamp;

    public PingMessage(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nonnull
    public Message build() {
        JsonElement body = new Gson().toJsonTree(this);
        return new Message(TYPE, body);
    }
}
