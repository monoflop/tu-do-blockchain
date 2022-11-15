package com.philippkutsch.tuchain.network.protocol.network;

import com.philippkutsch.tuchain.network.protocol.EncodeAbleMessage;

public class NewNodeMessage extends EncodeAbleMessage {
    public static final String TYPE = "new-node";

    private final String name;
    private final String host;
    private final int port;

    public NewNodeMessage(String name, String host, int port) {
        super(TYPE);
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
