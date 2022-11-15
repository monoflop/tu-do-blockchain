package com.philippkutsch.tuchain.network;

import com.philippkutsch.tuchain.jsonchain.Blockchain;

import javax.annotation.Nonnull;
import java.util.Queue;

public interface NodeController {
    @Nonnull
    Network getNetwork();

    @Nonnull
    Blockchain getBlockChain();

    @Nonnull
    Queue<PendingTransaction> getPendingTransactionQueue();

    void startMining();
    void stopMining();
}
