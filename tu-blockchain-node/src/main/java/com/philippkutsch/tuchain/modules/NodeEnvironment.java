package com.philippkutsch.tuchain.modules;

import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.Message;

import javax.annotation.Nonnull;

public interface NodeEnvironment {
    default boolean onMessage(
            @Nonnull RemoteNode remoteNode,
            @Nonnull Message message) {
        return false;
    }

    default void onNodeConnected(
            @Nonnull RemoteNode remoteNode) {
    }

    default void shutdown() throws Exception {

    }
}
