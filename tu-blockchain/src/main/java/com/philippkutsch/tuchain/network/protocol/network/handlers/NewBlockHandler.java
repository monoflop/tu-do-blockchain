package com.philippkutsch.tuchain.network.protocol.network.handlers;

import com.philippkutsch.tuchain.jsonchain.HashedBlock;
import com.philippkutsch.tuchain.network.protocol.network.NetworkMessageHandler;
import com.philippkutsch.tuchain.network.NodeController;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.network.messages.NewBlockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class NewBlockHandler implements NetworkMessageHandler<NewBlockMessage> {
    private static final Logger logger
            = LoggerFactory.getLogger(NewBlockHandler.class);

    @Override
    public void handleMessage(@Nonnull NodeController nodeController,
                              @Nonnull RemoteNode senderNode,
                              @Nonnull NewBlockMessage message) {
        HashedBlock newBlock = message.getBlock();

        //validate block data by block hash
        if(!newBlock.isHeaderValid()) {
            logger.error("Block hash invalid:\n" + newBlock);
            return;
        }

        //check if block fits into the chain (prev block hash and id matches)
        HashedBlock currentChainBlock;
        currentChainBlock = nodeController.getBlockChain().getLastBlock();

        if(currentChainBlock.getId() != newBlock.getId() - 1) {
            logger.error("Block id mismatch:\n" + newBlock);
            return;
        }

        if(!Arrays.equals(currentChainBlock.getHash(), newBlock.getPrevHash())) {
            logger.error("Block prevHash mismatch:\n" + newBlock);
            return;
        }

        //TODO Check if all transactions are valid

        //accept block
        //stop mining
        nodeController.stopMining();

        //Add new block to chain
        nodeController.getBlockChain().addBlock(newBlock);

        //TODO Remove transactions that was verified with the new block

        //start mining again
        nodeController.startMining();
    }
}
