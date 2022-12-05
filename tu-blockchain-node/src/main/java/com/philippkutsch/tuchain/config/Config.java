package com.philippkutsch.tuchain.config;

import java.util.List;

public class Config {
    private final String name;
    private final int port;
    private final String walletFilePath;
    private final String blockchainFilePath;
    private final List<Peer> knownPeers;

    public Config(String name, int port, String walletFilePath, String blockchainFilePath, List<Peer> knownPeers) {
        this.name = name;
        this.port = port;
        this.walletFilePath = walletFilePath;
        this.blockchainFilePath = blockchainFilePath;
        this.knownPeers = knownPeers;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getWalletFilePath() {
        return walletFilePath;
    }

    public String getBlockchainFilePath() {
        return blockchainFilePath;
    }

    public List<Peer> getKnownPeers() {
        return knownPeers;
    }
}
