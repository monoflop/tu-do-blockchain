package com.philippkutsch.tuchain;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.chain.SignedTransaction;
import com.philippkutsch.tuchain.chain.Transaction;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;

//Local implementation
public class Node extends LowLevelNode implements FutureCallback<HashedBlock> {
    private static final Logger logger
            = LoggerFactory.getLogger(Node.class);

    private final Queue<SignedTransaction> transactionQueue;
    private ListenableFuture<HashedBlock> miningFuture;

    public Node(@Nonnull Config config,
                @Nonnull RsaKeys rsaKeys,
                @Nonnull Blockchain blockchain)
            throws IOException {
        super(config, rsaKeys, blockchain);

        transactionQueue = new ConcurrentLinkedQueue<>();

        //Start mining with empty transaction queue
        startMining();
    }

    @Override
    public void onMessage(@Nonnull RemoteNode remoteNode,
                          @Nonnull Message message) {
        if(PingMessage.TYPE.equals(message.getType())) {
            network.send(remoteNode, new PongMessage().encode());
        }
        else if(BlockchainSyncMessage.TYPE.equals(message.getType())) {
            BlockchainSyncMessage syncMessage = message.to(BlockchainSyncMessage.class);
            checkAndUpdateBlockchain(remoteNode, syncMessage.getBlockchain());
            network.send(remoteNode, new BlockchainSyncMessage(blockchain).encode());
        }
        else if(NewBlockMessage.TYPE.equals(message.getType())) {
            NewBlockMessage newBlockMessage = message.to(NewBlockMessage.class);
            checkAndUpdateBlockchain(remoteNode, newBlockMessage.getHashedBlock());
        }
    }

    @Override
    public void onNodeConnected(@Nonnull RemoteNode remoteNode) {
        //For outgoing connections (we connected to remote node) try to sync blockchain
        //state with our blockchain
        if(!remoteNode.getConnectedNode().isIncomming()) {
            network.sendAndReceive(remoteNode, new BlockchainSyncMessage(blockchain).encode(), BlockchainSyncMessage.TYPE)
                    .whenComplete((message, throwable) -> {
                        if(message == null || throwable != null) {
                            logger.error("Blockchain sync error", throwable);
                            return;
                        }
                        BlockchainSyncMessage syncMessage = message.to(BlockchainSyncMessage.class);
                        checkAndUpdateBlockchain(remoteNode, syncMessage.getBlockchain());
                    });
        }
    }

    /**
     * Block was mined successfully
     */
    @Override
    public void onSuccess(@Nullable HashedBlock hashedBlock) {
        if(hashedBlock == null) {
            return;
        }

        //Add to blockchain
        blockchain.addBlock(hashedBlock);

        logger.debug("Block #" + hashedBlock.getId() + " mined: " + ChainUtils.encodeToString(hashedBlock));
        network.broadcast(new NewBlockMessage(hashedBlock).encode());

        //Start mining again
        startMining();
    }

    /**
     * Block mining exception
     */
    @Override
    public void onFailure(@Nonnull Throwable throwable) {
        if(throwable instanceof CancellationException) {
            logger.debug("Block mining failed (cancellation)");
        }
        else {
            logger.error("Block mining failed", throwable);
            startMining();
        }
    }

    @Override
    public void shutdown() throws IOException {
        super.shutdown();
    }

    private void checkAndUpdateBlockchain(
            @Nonnull RemoteNode remoteNode,
            @Nonnull HashedBlock hashedBlock) {
        List<HashedBlock> futureChain = new ArrayList<>(blockchain.getBlockchain());
        futureChain.add(hashedBlock);
        Blockchain newTargetChain = new Blockchain(futureChain);

        if(!newTargetChain.validateChain()) {
            logger.warn("Block " + hashedBlock.getId() + " doesnt fit in our chain");
            return;
        }

        //Add to chain
        logger.info("Node " + remoteNode.getKey() + " send new valid block #" + hashedBlock.getId());
        stopMining();
        blockchain.addBlock(hashedBlock);
        logger.info("Updated chain. Added block #" + hashedBlock.getId());
        startMining();
    }

    private void checkAndUpdateBlockchain(
            @Nonnull RemoteNode remoteNode,
            @Nonnull Blockchain remoteBlockChain) {
        List<HashedBlock> ownChain = blockchain.getBlockchain();
        List<HashedBlock> remoteChain = remoteBlockChain.getBlockchain();

        //Check if the blockchain is valid
        if(!remoteBlockChain.validateChain()) {
            logger.warn("Blockchain sync failed. Node " + remoteNode.getKey() + " send invalid chain");
            return;
        }

        logger.debug(ChainUtils.encodeToString(ownChain.get(0)));
        logger.debug(ChainUtils.encodeToString(remoteChain.get(0)));

        //Check if the genesis block is equal
        if(!ownChain.get(0).equals(remoteChain.get(0))) {
            logger.warn("Blockchain sync failed. Node " + remoteNode.getKey() + " send invalid chain with genesis mismatch");
            return;
        }

        //Check if the target chain is longer than ours
        if(ownChain.size() < remoteChain.size()) {
            logger.info("Node " + remoteNode.getKey() + " has an longer valid chain");
            stopMining();

            //TODO remove all transactions from queue that are included in this blocks

            int additionalBlocks = remoteChain.size() - ownChain.size();
            List<HashedBlock> newBlocks = remoteChain.subList(
                    (remoteChain.size() - additionalBlocks),
                    remoteChain.size());
            for(HashedBlock newBlock : newBlocks) {
                logger.debug("Adding block #" + newBlock.getId());
                blockchain.getBlockchain().add(newBlock);
            }

            logger.info("Updated chain. Added " + additionalBlocks + " blocks");

            startMining();
        }
        else {
            logger.debug("Chains are equal");
        }
    }

    private void startMining() {
        logger.debug("Block mining starting");

        //Collect transactions
        List<SignedTransaction> signedTransactionList = new ArrayList<>(transactionQueue.stream().toList());

        //Create mining reward transaction
        Transaction.Target target = new Transaction.Target(100, rsaKeys.getPublicKeyBytes());
        Transaction transaction = new Transaction(0, System.currentTimeMillis(), "coinbase".getBytes(StandardCharsets.UTF_8),
                new Transaction.Target[]{target});
        byte[] transactionBytes = ChainUtils.encodeToBytes(transaction);
        byte[] signature;
        try {
            signature = rsaKeys.signData(transactionBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return;
        }

        SignedTransaction miningRewardTransaction = SignedTransaction.fromTransaction(transaction, signature);
        signedTransactionList.add(0, miningRewardTransaction);

        //TODO: Fix transaction ids. ids are ordered by timestamp, so id should not be part
        //  of the signature...

        miningFuture = service.submit(new Miner(blockchain
                .buildNextBlock(signedTransactionList), null));
        Futures.addCallback(miningFuture, this, service);
    }

    private void stopMining() {
        logger.debug("Block mining stopping");
        if(miningFuture != null) {
            miningFuture.cancel(true);
            miningFuture = null;
        }
    }
}
