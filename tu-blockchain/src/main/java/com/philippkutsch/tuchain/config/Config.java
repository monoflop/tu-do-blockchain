package com.philippkutsch.tuchain.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class Config {
    private final String blockchainFile;
    private final List<Node> nodeList;

    public Config(@Nullable String blockchainFile,
                  @Nonnull List<Node> nodeList) {
        this.blockchainFile = blockchainFile;
        this.nodeList = nodeList;
    }

    @Nullable
    public String getBlockchainFile() {
        return blockchainFile;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }
}
