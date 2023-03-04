package com.philippkutsch.tuchain.modules.mining;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.philippkutsch.tuchain.Node;
import com.philippkutsch.tuchain.chain.Contract;
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
    private final Queue<Contract> contractQueue;
    private ListenableFuture<HashedBlock> miningFuture;

    public MiningModule(@Nonnull Node node)
            throws ModuleLoadException {
        super(node);
        this.transactionQueue = new ConcurrentLinkedQueue<>();
        this.contractQueue = new ConcurrentLinkedQueue<>();

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
        //TODO: Consider current queue for verification
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

    public boolean submitContract(@Nonnull Contract contract) {
        //TODO: Validate
        //Check if transaction is already in queue
        boolean alreadyInQueue = contractQueue.stream()
                .anyMatch((c) -> Arrays.equals(c.getContractId(), contract.getContractId()));
        if(alreadyInQueue) {
            logger.error("Contract already queued");
            return false;
        }

        contractQueue.add(contract);
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

    public void removeContractsFromQueue(@Nonnull Contract[] contracts) {
        contractQueue.removeIf((c -> {
            for(Contract contract : contracts) {
                if(Arrays.equals(c.getContractId(), contract.getContractId())) {
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

        //Collect transactions and contracts
        List<Transaction> transactionList = new ArrayList<>(transactionQueue.stream().toList());
        List<Contract> contractList = new ArrayList<>(contractQueue.stream().toList());

        //Purge transactions and contract
        transactionQueue.clear();
        contractQueue.clear();

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

        contractList.sort(Comparator.comparing(Contract::getTimestamp));

        miningFuture = node.getService().submit(new Miner(24,0, node.getBlockchain()
                .buildNextBlock(transactionList, contractList), null));
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
