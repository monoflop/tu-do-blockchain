package com.philippkutsch.tuchain.chain;

import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;

public class UnspentTransactionOutput {
    private final byte[] txId;
    private final int vOut;
    private final int amount;

    public UnspentTransactionOutput(
            @Nonnull byte[] txId,
            int vOut,
            int amount) {
        this.txId = txId;
        this.vOut = vOut;
        this.amount = amount;
    }

    public byte[] getTxId() {
        return txId;
    }

    public int getvOut() {
        return vOut;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "" + ChainUtils.bytesToBase64(txId) + " [" + vOut + "] " + amount;
    }
}
