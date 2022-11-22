package com.philippkutsch.tuchain.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class RemoteNode {
    private static final Logger logger
            = LoggerFactory.getLogger(RemoteNode.class);
    private final ConnectedNode connectedNode;

    public RemoteNode(@Nonnull ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
    }

    public ConnectedNode getConnectedNode() {
        return connectedNode;
    }

    @Nonnull
    public String getKey() {
        return connectedNode.getKey();
    }
}
