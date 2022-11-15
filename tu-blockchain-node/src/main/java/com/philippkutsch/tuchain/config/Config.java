package com.philippkutsch.tuchain.config;

import java.util.List;

public class Config {
    private final String name;
    private final int port;
    private final String privateKeyFilePath;
    private final String publicKeyFilePath;
    private final String blockchainFilePath;
    private final List<Peer> knownPeers;

    public Config(String name, int port, String privateKeyFilePath, String publicKeyFilePath, String blockchainFilePath, List<Peer> knownPeers) {
        this.name = name;
        this.port = port;
        this.privateKeyFilePath = privateKeyFilePath;
        this.publicKeyFilePath = publicKeyFilePath;
        this.blockchainFilePath = blockchainFilePath;
        this.knownPeers = knownPeers;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getPrivateKeyFilePath() {
        return privateKeyFilePath;
    }

    public String getPublicKeyFilePath() {
        return publicKeyFilePath;
    }

    public String getBlockchainFilePath() {
        return blockchainFilePath;
    }

    public List<Peer> getKnownPeers() {
        return knownPeers;
    }

    /*@Nonnull
    public static Config standard() {
        List<Peer> knownPeers = new ArrayList<>();
        //knownPeers.add(new Peer("localhost", 8000));
        return new Config(UUID.randomUUID().toString(), 8000, null, null, null, knownPeers);
    }*/
}
