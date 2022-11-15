package com.philippkutsch.tuchain.network;

import javax.annotation.Nonnull;

public class PendingTransaction {
    private final long timestamp;
    private final String sourceWallet;
    private final String targetWallet;
    private final int amount;
    private final String signature;

    public PendingTransaction(long timestamp,
                              @Nonnull String sourceWallet,
                              @Nonnull String targetWallet,
                              int amount,
                              @Nonnull String signature) {
        this.timestamp = timestamp;
        this.sourceWallet = sourceWallet;
        this.targetWallet = targetWallet;
        this.amount = amount;
        this.signature = signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSourceWallet() {
        return sourceWallet;
    }

    public String getTargetWallet() {
        return targetWallet;
    }

    public int getAmount() {
        return amount;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isValid() {
        return true;
    }
}
