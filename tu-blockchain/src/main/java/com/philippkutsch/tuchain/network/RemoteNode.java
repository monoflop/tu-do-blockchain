package com.philippkutsch.tuchain.network;

import com.philippkutsch.tuchain.network.protocol.Message;

import javax.annotation.Nonnull;

public interface RemoteNode {
    @Nonnull
    String getNodeId();
    void sendMessage(@Nonnull RemoteNode senderNode, @Nonnull Message message);
    void sendUserMessage(@Nonnull Message message);
    void shutdown();
}
