package com.philippkutsch.tuchain.network.protocol.user.messages;

import com.philippkutsch.tuchain.network.protocol.EncodeAbleMessage;

public class RequestBlockMessage extends EncodeAbleMessage {
    public static final String TYPE = "block";

    private final long id;

    public RequestBlockMessage(long id) {
        super(TYPE);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
