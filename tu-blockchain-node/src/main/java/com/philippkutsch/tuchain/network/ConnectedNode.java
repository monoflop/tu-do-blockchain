package com.philippkutsch.tuchain.network;

import javax.annotation.Nonnull;

/**
 * Wrapper for NetworkConnection
 *
 * Contains additional information received during handshake.
 */
public class ConnectedNode {
    private final NetworkConnection connection;
    private final String name;
    private final String host;
    private final int port;
    private final boolean incomming;

    public ConnectedNode(@Nonnull NetworkConnection connection,
                         @Nonnull String name,
                         @Nonnull String host,
                         int port,
                         boolean incomming) {
        this.connection = connection;
        this.name = name;
        this.host = host;
        this.port = port;
        this.incomming = incomming;
    }

    public NetworkConnection getConnection() {
        return connection;
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

    public boolean isIncomming() {
        return incomming;
    }

    @Nonnull
    public String getKey() {
        return host + ":" + port;
    }
}
