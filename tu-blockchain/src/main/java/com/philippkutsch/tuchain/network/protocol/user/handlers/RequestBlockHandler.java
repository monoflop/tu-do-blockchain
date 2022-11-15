package com.philippkutsch.tuchain.network.protocol.user.handlers;

import com.philippkutsch.tuchain.jsonchain.HashedBlock;
import com.philippkutsch.tuchain.network.NodeController;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.user.UserMessageHandler;
import com.philippkutsch.tuchain.network.protocol.user.messages.RequestBlockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public class RequestBlockHandler implements UserMessageHandler<RequestBlockMessage> {
    private static final Logger logger
            = LoggerFactory.getLogger(RequestBlockHandler.class);

    @Override
    public void handleMessage(@Nonnull NodeController nodeController,
                              @Nonnull RemoteNode currentNode,
                              @Nonnull RequestBlockMessage message) {
        List<HashedBlock> blockList = nodeController.getBlockChain().getBlockchain();
        boolean found = false;
        for(HashedBlock block : blockList) {
            if(block.getId() == message.getId()) {
                logger.info("\n" + block);
                if(block.getId() != 1) {
                    logger.info("\n" + block.getData());
                }
                found = true;
                break;
            }
        }

        if(!found) {
            logger.warn("Block #" + message.getId() + " not found");
        }
    }
}
