package com.philippkutsch.tuchain.network.protocol;

public class PongMessage extends EncodeAbleMessage {
    public static final String TYPE = "pong";

    public PongMessage() {
        super(TYPE);
    }
}
