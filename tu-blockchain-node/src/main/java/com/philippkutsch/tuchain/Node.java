package com.philippkutsch.tuchain;

import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.network.protocol.Message;
import com.philippkutsch.tuchain.network.RemoteNode;

import javax.annotation.Nonnull;

//Local implementation
public class Node extends LowLevelNode {
    public Node(@Nonnull Config config, @Nonnull RsaKeys rsaKeys) {
        super(config, rsaKeys);
    }

    @Override
    public void onMessage(@Nonnull RemoteNode remoteNode,
                          @Nonnull Message message) {

    }
}
