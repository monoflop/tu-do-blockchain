package com.philippkutsch.tuchain.network.protocol.network;

import com.philippkutsch.tuchain.network.NodeController;
import com.philippkutsch.tuchain.network.RemoteNode;

import javax.annotation.Nonnull;

public interface NetworkMessageHandler<T> {
    void handleMessage(@Nonnull NodeController nodeController,
                       @Nonnull RemoteNode senderNode,
                       @Nonnull T message);
}
