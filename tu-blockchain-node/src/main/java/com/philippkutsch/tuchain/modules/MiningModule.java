package com.philippkutsch.tuchain.modules;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.philippkutsch.tuchain.Node;
import com.philippkutsch.tuchain.Miner;
import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.chain.SignedTransaction;
import com.philippkutsch.tuchain.chain.Transaction;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.network.protocol.NewBlockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MiningModule extends NodeModule implements FutureCallback<HashedBlock>  {
    private static final Logger logger
            = LoggerFactory.getLogger(MiningModule.class);

    private final Queue<SignedTransaction> transactionQueue;
    private ListenableFuture<HashedBlock> miningFuture;

    public MiningModule(@Nonnull Node node)
            throws ModuleLoadException {
        super(node);
        this.transactionQueue = new ConcurrentLinkedQueue<>();

        startMining();
    }

    /**
     * Block was mined successfully
     */
    @Override
    public void onSuccess(@Nullable HashedBlock hashedBlock) {
        if(hashedBlock == null) {
            return;
        }

        miningFuture = null;

        //Add to blockchain
        node.getBlockchain().addBlock(hashedBlock);

        logger.debug("Block #" + hashedBlock.getId() + " mined: " + ChainUtils.encodeToString(hashedBlock));
        node.getNetwork().broadcast(new NewBlockMessage(hashedBlock).encode());

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
    public void shutdown() throws Exception {
        stopMining();
    }

    public void startMining() {
        if(miningFuture != null) {
            logger.warn("Block mining already running");
            return;
        }

        logger.debug("Block mining starting");
        //Collect transactions
        List<SignedTransaction> signedTransactionList = new ArrayList<>(transactionQueue.stream().toList());

        //Create mining reward transaction
        Transaction.Target target = new Transaction.Target(100, node.getRsaKeys().getPublicKeyBytes());
        Transaction transaction = new Transaction(0, System.currentTimeMillis(), "coinbase".getBytes(StandardCharsets.UTF_8),
                new Transaction.Target[]{target});
        byte[] transactionBytes = ChainUtils.encodeToBytes(transaction);
        byte[] signature;
        try {
            signature = node.getRsaKeys().signData(transactionBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return;
        }

        SignedTransaction miningRewardTransaction = SignedTransaction.fromTransaction(transaction, signature);
        signedTransactionList.add(0, miningRewardTransaction);

        //TODO: Fix transaction ids. ids are ordered by timestamp, so id should not be part
        //  of the signature...

        miningFuture = node.getService().submit(new Miner(node.getBlockchain()
                .buildNextBlock(signedTransactionList), null));
        Futures.addCallback(miningFuture, this, node.getService());
    }

    public void stopMining() {
        logger.debug("Block mining stopping");
        if(miningFuture != null) {
            miningFuture.cancel(true);
            miningFuture = null;
        }
    }
}
