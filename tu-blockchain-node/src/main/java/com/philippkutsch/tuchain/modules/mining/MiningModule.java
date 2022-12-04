package com.philippkutsch.tuchain.modules.mining;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.philippkutsch.tuchain.Node;
import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.chain.Transaction;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.modules.ModuleLoadException;
import com.philippkutsch.tuchain.modules.NodeModule;
import com.philippkutsch.tuchain.network.protocol.NewBlockMessage;
import com.philippkutsch.tuchain.utils.TransactionVerificationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MiningModule extends NodeModule implements FutureCallback<HashedBlock>  {
    private static final Logger logger
            = LoggerFactory.getLogger(MiningModule.class);

    private final Queue<Transaction> transactionQueue;
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

    public boolean submitTransaction(@Nonnull Transaction transaction) {
        //Validate transaction
        TransactionVerificationUtils.VerificationResult result =
                TransactionVerificationUtils.verifyTransaction(
                        node.getBlockchain(),
                        transaction,
                        true);
        if(!result.isSuccess()) {
            logger.error("Transaction invalid " + result.getError());
            return false;
        }

        //Check if transaction is already in queue
        boolean alreadyInQueue = transactionQueue.stream()
                .anyMatch((t) -> Arrays.equals(t.getTransactionId(), transaction.getTransactionId()));
        if(alreadyInQueue) {
            logger.error("Transaction already queued");
            return false;
        }

        transactionQueue.add(transaction);

        return true;
    }

    public void removeTransactionsFromQueue(@Nonnull Transaction[] transactions) {
        transactionQueue.removeIf((t -> {
            for(Transaction transaction : transactions) {
                if(Arrays.equals(t.getTransactionId(), transaction.getTransactionId())) {
                    return true;
                }
            }
            return false;
        }));
    }

    public void startMining() {
        if(miningFuture != null) {
            logger.warn("Block mining already running");
            return;
        }

        logger.debug("Block mining starting");

        //Collect transactions
        List<Transaction> transactionList = new ArrayList<>(transactionQueue.stream().toList());

        //Purge transactions
        transactionQueue.clear();

        //Generate coinbase transaction
        Transaction coinbase = Transaction.buildCoinbaseTransaction(
                System.currentTimeMillis(),
                //TODO read from config
                100,
                "PK coinbase transaction".getBytes(StandardCharsets.UTF_8),
                node.getRsaKeys().getPublicKeyBytes());

        //combine and order by timestamp descending
        transactionList.sort(Comparator.comparing(Transaction::getTimestamp));
        transactionList.add(0, coinbase);

        miningFuture = node.getService().submit(new Miner(node.getBlockchain()
                .buildNextBlock(transactionList), null));
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
