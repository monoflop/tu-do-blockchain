package com.philippkutsch.tuchain.network.protocol.user.handlers;

import com.philippkutsch.tuchain.network.NodeController;
import com.philippkutsch.tuchain.network.PendingTransaction;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.user.UserMessageHandler;
import com.philippkutsch.tuchain.network.protocol.user.messages.PublishTransactionMessage;

import javax.annotation.Nonnull;

public class PublishTransactionHandler implements UserMessageHandler<PublishTransactionMessage> {
    @Override
    public void handleMessage(@Nonnull NodeController nodeController,
                              @Nonnull RemoteNode currentNode,
                              @Nonnull PublishTransactionMessage message) {
        //TODO Validate transaction

        //Broadcast new transactions to other nodes
        nodeController.getNetwork().broadcast(currentNode, message.encode());

        //Add transaction to queue
        nodeController.getPendingTransactionQueue().add(new PendingTransaction(
                System.currentTimeMillis(),
                message.getSourceWallet(),
                message.getTargetWallet(),
                message.getAmount(),
                message.getSignature()));
    }
}
