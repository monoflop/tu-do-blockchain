package com.philippkutsch.tuchain.network.protocol.user;

import com.philippkutsch.tuchain.network.NodeController;
import com.philippkutsch.tuchain.network.RemoteNode;

import javax.annotation.Nonnull;

public interface UserMessageHandler<T> {
    void handleMessage(@Nonnull NodeController nodeController,
                       @Nonnull RemoteNode currentNode,
                       @Nonnull T message);
}
