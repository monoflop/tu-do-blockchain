package com.philippkutsch.tuchain.modules;

import com.philippkutsch.tuchain.Node;
import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.BlockchainSyncMessage;
import com.philippkutsch.tuchain.network.protocol.Message;
import com.philippkutsch.tuchain.network.protocol.NewBlockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

//TODO chain is not synched correctly
public class BlockchainSyncModule extends NodeModule {
    private static final Logger logger
            = LoggerFactory.getLogger(BlockchainSyncModule.class);

    public BlockchainSyncModule(
            @Nonnull Node node)
            throws ModuleLoadException {
        super(node);
    }

    @Override
    protected Class<? extends NodeModule>[] loadDependencies() {
        //noinspection unchecked
        return new Class[]{
                MiningModule.class};
    }

    @Override
    public boolean onMessage(
            @Nonnull RemoteNode remoteNode,
            @Nonnull Message message) {
        if(BlockchainSyncMessage.TYPE.equals(message.getType())) {
            BlockchainSyncMessage syncMessage = message.to(BlockchainSyncMessage.class);
            handleSync(remoteNode, syncMessage);
            node.getNetwork().send(remoteNode, new BlockchainSyncMessage(node.getBlockchain()).encode());
            return true;
        }
        else if(NewBlockMessage.TYPE.equals(message.getType())) {
            NewBlockMessage newBlockMessage = message.to(NewBlockMessage.class);
            boolean success = tryAddingNewBlock(newBlockMessage.getHashedBlock());
            if(success) {
                logger.debug("New block from " + remoteNode.getKey()
                        + ". Updated chain. Added block #"
                        + newBlockMessage.getHashedBlock().getId());
            }
            else {
                logger.debug("New block from " + remoteNode.getKey()
                        + ". Block invalid and dont fit #"
                        + newBlockMessage.getHashedBlock().getId());
            }
            return true;
        }
        return false;
    }

    @Override
    public void onNodeConnected(@Nonnull RemoteNode remoteNode) {
        //For outgoing connections (we connected to remote node) try to sync blockchain
        //state with our blockchain
        if(!remoteNode.getConnectedNode().isIncomming()) {
            node.getNetwork().sendAndReceive(remoteNode, new BlockchainSyncMessage(node.getBlockchain()).encode(), BlockchainSyncMessage.TYPE)
                    .whenComplete((message, throwable) -> {
                        if(message == null || throwable != null) {
                            logger.error("Blockchain sync error", throwable);
                            return;
                        }
                        BlockchainSyncMessage syncMessage = message.to(BlockchainSyncMessage.class);
                        handleSync(remoteNode, syncMessage);
                    });
        }
    }

    private void handleSync(@Nonnull RemoteNode remoteNode,
                            @Nonnull BlockchainSyncMessage blockchainSyncMessage) {
        boolean success = tryBlockchainSync(blockchainSyncMessage.getBlockchain());
        if(success) {
            logger.debug("Node " + remoteNode.getKey() + " send valid chain. Blockchain sync successful");
        }
        else {
            logger.debug("Node " + remoteNode.getKey() + " send invalid chain.");
        }
    }

    private boolean tryAddingNewBlock(@Nonnull HashedBlock hashedBlock) {
        MiningModule miningModule = node.requireModule(MiningModule.class);

        //Lock chain
        node.getBlockchain().beginWriteAccess();
        try {
            //Validate
            List<HashedBlock> futureChain = node.getBlockchain().getBlockchain();
            futureChain.add(hashedBlock);
            Blockchain newTargetChain = new Blockchain(futureChain);
            if(!newTargetChain.validateChain()) {
                logger.debug("Chain with added block invalid");
                return false;
            }

            //Add to chain
            miningModule.stopMining();
            node.getBlockchain().addBlock(hashedBlock);
            miningModule.startMining();
            return true;
        }
        //Unlock chain
        finally {
            node.getBlockchain().endWriteAccess();
        }
    }

    private boolean tryBlockchainSync(
            @Nonnull Blockchain remoteBlockChain) {
        MiningModule miningModule = node.requireModule(MiningModule.class);

        //Lock chain
        node.getBlockchain().beginWriteAccess();
        try {
            List<HashedBlock> ownChain = node.getBlockchain().getBlockchain();
            List<HashedBlock> remoteChain = remoteBlockChain.getBlockchain();

            //Check if the blockchain is valid
            if(!remoteBlockChain.validateChain()) {
                logger.debug("Remote chain invalid");
                return false;
            }

            //Check if the genesis block is equal
            if(!ownChain.get(0).equals(remoteChain.get(0))) {
                logger.debug("Genesis block mismatch");
                return false;
            }

            //Check if the target chain is longer than ours
            if(ownChain.size() < remoteChain.size()) {
                logger.debug("Remote chain is longer than ours");
                miningModule.stopMining();

                //TODO remove all transactions from queue that are included in this blocks

                int additionalBlocks = remoteChain.size() - ownChain.size();
                List<HashedBlock> newBlocks = remoteChain.subList(
                        (remoteChain.size() - additionalBlocks),
                        remoteChain.size());
                for(HashedBlock newBlock : newBlocks) {
                    node.getBlockchain().addBlock(newBlock);
                }
                miningModule.startMining();
            }
            else {
                logger.debug("Chains have equal size");
            }
            return true;
        }
        //Unlock chain
        finally {
            node.getBlockchain().endWriteAccess();
        }
    }
}
