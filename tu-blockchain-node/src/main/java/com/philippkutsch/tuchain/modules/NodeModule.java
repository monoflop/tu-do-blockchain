package com.philippkutsch.tuchain.modules;

import com.philippkutsch.tuchain.Node;
import com.philippkutsch.tuchain.network.Network;

import javax.annotation.Nonnull;

public abstract class NodeModule implements Network.Listener {
    private final Node node;

    public NodeModule(@Nonnull Node node) {
        this.node = node;
    }
}
