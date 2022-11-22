package com.philippkutsch.tuchain.network.protocol;

import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;

public abstract class EncodeAbleMessage {
    private final transient String type;

    public EncodeAbleMessage(@Nonnull String type) {
        this.type = type;
    }

    @Nonnull
    public Message encode() {
        return new Message(type, ChainUtils.GSON.toJsonTree(this));
    }
}