package com.philippkutsch.tuchain.modules;

import com.philippkutsch.tuchain.Node;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.Message;
import com.philippkutsch.tuchain.network.protocol.PingMessage;
import com.philippkutsch.tuchain.network.protocol.PongMessage;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * PingModule
 *
 * Allows sending ping messages to other nodes and wait for a pong response.
 */
public class PingModule extends NodeModule {
    public PingModule(@Nonnull Node node)
            throws ModuleLoadException {
        super(node);
    }

    @Override
    public boolean onMessage(@Nonnull RemoteNode remoteNode, @Nonnull Message message) {
        if(PingMessage.TYPE.equals(message.getType())) {
            node.getNetwork().send(remoteNode, new PongMessage().encode());
            return true;
        }
        return false;
    }

    public boolean pingNode(@Nonnull RemoteNode remoteNode) {
        try {
            Message message = node.getNetwork()
                    .sendAndReceive(remoteNode, new PingMessage().encode(), PongMessage.TYPE)
                    .get(30, TimeUnit.SECONDS);
            return message != null && PongMessage.TYPE.equals(message.getType());
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }
}
