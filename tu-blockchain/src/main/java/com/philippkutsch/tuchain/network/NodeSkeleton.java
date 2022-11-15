package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.philippkutsch.tuchain.blockchain.RsaKeys;
import com.philippkutsch.tuchain.jsonchain.*;
import com.philippkutsch.tuchain.jsonchain.utils.ChainUtils;
import com.philippkutsch.tuchain.network.protocol.Message;
import com.philippkutsch.tuchain.network.protocol.network.NetworkMessageHandler;
import com.philippkutsch.tuchain.network.protocol.network.NetworkMessageHandlerContainer;
import com.philippkutsch.tuchain.network.protocol.network.messages.NewBlockMessage;
import com.philippkutsch.tuchain.network.protocol.user.UserMessageHandler;
import com.philippkutsch.tuchain.network.protocol.user.UserMessageHandlerContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;

public class NodeSkeleton implements NodeController, RemoteNode {
    private static final Logger logger
            = LoggerFactory.getLogger(NodeSkeleton.class);

    private static final Gson GSON = new Gson();
    private final String id;
    private final ExecutorService executorService;
    private final ListeningExecutorService service;

    private final Network network;
    private final RsaKeys rsaKeys;

    private final Blockchain blockchain;
    private final Miner miner;
    private final Queue<PendingTransaction> pendingTransactionQueue;

    private final List<NetworkMessageHandlerContainer<?>> networkMessageHandlerList;
    private final List<UserMessageHandlerContainer<?>> userMessageHandlerList;

    public NodeSkeleton(@Nonnull Network network,
                        @Nonnull String id,
                        @Nonnull RsaKeys rsaKeys,
                        @Nonnull List<HashedBlock> initialBlockchain) {
        this.id = id;
        this.executorService = Executors.newCachedThreadPool();
        this.service = MoreExecutors.listeningDecorator(executorService);
        this.network = network;
        this.rsaKeys = rsaKeys;

        this.blockchain = new Blockchain(initialBlockchain);
        //this.blockchain = new Blockchain(ChainUtils.decodeFromString(
        //        "{\"timestamp\":1667745075755,\"nuOnce\":24203771,\"hash\":\"AAAAj35/ExBxatM/3hNzZqt2mANd+g9/1j0GNvwEdL8\\u003d\",\"id\":1,\"prevHash\":\"\",\"data\":{\"signedTransactions\":[]}}", HashedBlock.class));

        this.miner = new Miner();
        this.pendingTransactionQueue = new SynchronousQueue<>();

        this.networkMessageHandlerList = new ArrayList<>();
        this.userMessageHandlerList = new ArrayList<>();

        logger.info("Starting testing node " + id);

        //Start mining
        startMining();
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> void registerNetworkMessageHandler(
            @Nonnull Class<T> targetClass,
            @Nonnull String type,
            @Nonnull NetworkMessageHandler<T> handler) {
        NetworkMessageHandlerContainer<T> container
                = new NetworkMessageHandlerContainer<>(targetClass, type, handler);
        this.networkMessageHandlerList.add(container);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> void registerUserMessageHandler(
            @Nonnull Class<T> targetClass,
            @Nonnull String type,
            @Nonnull UserMessageHandler<T> handler) {
        UserMessageHandlerContainer<T> container
                = new UserMessageHandlerContainer<>(targetClass, type, handler);
        this.userMessageHandlerList.add(container);
    }

    @SuppressWarnings("rawtypes")
    private void handleMessage(@Nonnull RemoteNode senderNode, @Nonnull Message message) {
        logger.debug("Node " + id + " Received message " + message.getType() +  ": " + GSON.toJson(message.getType()) +  " from " + senderNode.getNodeId());

        boolean messageHandled = false;
        for(NetworkMessageHandlerContainer handler : networkMessageHandlerList) {
            if(handler.getType().equals(message.getType())) {
                //noinspection unchecked
                handler.getMessageHandler().handleMessage(this, senderNode, message.to(handler.getMessageClass()));
                messageHandled = true;
            }
        }

        if(!messageHandled) {
            logger.warn("No handler registered for message type " + message.getType());
        }
    }

    @SuppressWarnings("rawtypes")
    private void handleUserMessage(@Nonnull Message message) {
        logger.debug("Node " + id + " Received user message " + message.getType() + ": " + GSON.toJson(message.getType()));

        boolean messageHandled = false;
        for(UserMessageHandlerContainer handler : userMessageHandlerList) {
            if(handler.getType().equals(message.getType())) {
                //noinspection unchecked
                handler.getMessageHandler().handleMessage(this, this, message.to(handler.getMessageClass()));
                messageHandled = true;
            }
        }

        if(!messageHandled) {
            logger.warn("No handler registered for message type " + message.getType());
        }
    }

    private void onBlockMined(@Nonnull HashedBlock hashedBlock) {
        logger.debug("Node " + id + " successfully mined block #" + hashedBlock.getId());
        logger.debug("\n" + ChainUtils.encodeToString(hashedBlock));

        blockchain.addBlock(hashedBlock);

        //TODO Remove all mined transactions from queue

        //publish block to network
        NewBlockMessage newBlockMessage = new NewBlockMessage(hashedBlock);
        network.broadcast(this, newBlockMessage.encode());

        //Start mining again
        startMining();
    }

    @Nonnull
    @Override
    public Network getNetwork() {
        return network;
    }

    @Nonnull
    @Override
    public Blockchain getBlockChain() {
        return blockchain;
    }

    @Nonnull
    @Override
    public Queue<PendingTransaction> getPendingTransactionQueue() {
        return pendingTransactionQueue;
    }

    @Override
    public void startMining() {
        HashedBlock prevBlock = blockchain.getLastBlock();

        //Create target block
        //Construct block body
        //If we have a keypair, we add a mining reward transaction
        Transaction.Target target = new Transaction.Target(100, rsaKeys.getPublicKeyBytes());
        Transaction transaction = new Transaction(0, System.currentTimeMillis(), new byte[0],
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

        //TODO Collect transactions
        SignedTransaction[] signedTransactions = new SignedTransaction[]{miningRewardTransaction};
        BlockBody blockBody = new BlockBody(signedTransactions);

        Block block = new Block(prevBlock.getId() + 1, prevBlock.getHash(), blockBody);

        miner.startMining(executorService, block, null)
                .whenComplete((hashedBlock, exception) -> {
                    //TODO something is wrong with future handling. Should work without stop
                    if (exception != null) {
                        //CancellationException is thrown on manual cancel
                        if(!(exception instanceof CancellationException)) {
                            logger.error("mining exception", exception);
                        }
                    } else {
                        miner.stopMining();
                        onBlockMined(hashedBlock);
                    }
                });
    }

    @Override
    public void stopMining() {
        miner.stopMining();
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down node " + getNodeId());
        this.executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Shutdown failed");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Nonnull
    @Override
    public String getNodeId() {
        return id;
    }

    //Receive messages from other nodes
    @Override
    public void sendMessage(@Nonnull RemoteNode senderNode, @Nonnull Message message) {
        //Push message to service. Messages are handled async
        //noinspection ResultOfMethodCallIgnored
        service.submit(() -> handleMessage(senderNode, message));
    }

    //Receive messages from users
    @Override
    public void sendUserMessage(@NotNull Message message) {
        //Push message to service. Messages are handled async
        //noinspection ResultOfMethodCallIgnored
        service.submit(() -> handleUserMessage(message));
    }
}