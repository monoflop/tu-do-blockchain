package com.philippkutsch.tuchain.network.protocol;

import com.google.gson.JsonElement;
import com.philippkutsch.tuchain.jsonchain.utils.ChainUtils;

import javax.annotation.Nonnull;

public class Message {
    final String type;
    final JsonElement body;

    public Message(@Nonnull String type, @Nonnull JsonElement body) {
        this.type = type;
        this.body = body;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    @Nonnull
    public JsonElement getBody() {
        return body;
    }

    @Nonnull
    public <T> T to(@Nonnull Class<T> clazz) {
        return ChainUtils.GSON.fromJson(body, clazz);
    }
}
