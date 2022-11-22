package com.philippkutsch.tuchain.network.protocol.network;

import com.philippkutsch.tuchain.config.Peer;
import com.philippkutsch.tuchain.network.protocol.EncodeAbleMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HandshakeMessage extends EncodeAbleMessage {
    public static final String TYPE = "handshake";
    private final int version;
    private final String name;
    private final String host;
    private final int port;
    private final List<Peer> knownPeers;

    public HandshakeMessage(int version, String name, String host, int port, List<Peer> knownPeers) {
        super(TYPE);
        this.version = version;
        this.name = name;
        this.host = host;
        this.port = port;
        this.knownPeers = knownPeers;
    }

    public int getVersion() {
        return version;
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

    public List<Peer> getKnownPeers() {
        return knownPeers;
    }
}
