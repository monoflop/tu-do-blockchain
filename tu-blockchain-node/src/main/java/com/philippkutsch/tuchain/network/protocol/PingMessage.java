package com.philippkutsch.tuchain.network.protocol;

public class PingMessage extends EncodeAbleMessage {
    public static final String TYPE = "ping";

    public PingMessage() {
        super(TYPE);
    }
}
