package com.philippkutsch.tuchain.network;

import com.philippkutsch.tuchain.blockchain.RsaKeys;
import com.philippkutsch.tuchain.jsonchain.HashedBlock;
import com.philippkutsch.tuchain.network.protocol.network.handlers.NewBlockHandler;
import com.philippkutsch.tuchain.network.protocol.network.messages.NewBlockMessage;
import com.philippkutsch.tuchain.network.protocol.user.handlers.BalanceMessageHandler;
import com.philippkutsch.tuchain.network.protocol.user.handlers.PublishTransactionHandler;
import com.philippkutsch.tuchain.network.protocol.user.handlers.RequestBlockHandler;
import com.philippkutsch.tuchain.network.protocol.user.messages.BalanceMessage;
import com.philippkutsch.tuchain.network.protocol.user.messages.PublishTransactionMessage;
import com.philippkutsch.tuchain.network.protocol.user.messages.RequestBlockMessage;

import javax.annotation.Nonnull;
import java.util.List;

public class TestingNode extends NodeSkeleton {
    public TestingNode(
            @Nonnull Network network,
            @Nonnull String id,
            @Nonnull RsaKeys rsaKeys,
            @Nonnull List<HashedBlock> initialBlockchain) {
        super(network, id, rsaKeys, initialBlockchain);

        registerNetworkMessageHandler(
                NewBlockMessage.class,
                NewBlockMessage.TYPE,
                new NewBlockHandler());

        registerUserMessageHandler(BalanceMessage.class,
                BalanceMessage.TYPE, new BalanceMessageHandler());

        registerUserMessageHandler(PublishTransactionMessage.class,
                PublishTransactionMessage.TYPE, new PublishTransactionHandler());

        registerUserMessageHandler(RequestBlockMessage.class,
                RequestBlockMessage.TYPE, new RequestBlockHandler());
    }
}
