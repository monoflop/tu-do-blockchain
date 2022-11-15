package com.philippkutsch.tuchain.config;

import javax.annotation.Nonnull;

public class Node {
    public enum NodeType {
        normal,
        fraudulent
    }

    private final String id;
    private final NodeType type;
    private final String privateKeyFile;
    private final String publicKeyFile;

    public Node(@Nonnull String id,
                @Nonnull NodeType type,
                @Nonnull String privateKeyFile,
                @Nonnull String publicKeyFile) {
        this.id = id;
        this.type = type;
        this.privateKeyFile = privateKeyFile;
        this.publicKeyFile = publicKeyFile;
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public String getPublicKeyFile() {
        return publicKeyFile;
    }
}
