package com.philippkutsch.tuchain.jsonchain;

import javax.annotation.Nonnull;

public class SignedTransaction extends Transaction {
    protected final byte[] signature;

    public SignedTransaction(
            long id,
            long timestamp,
            @Nonnull byte[] pubKey,
            @Nonnull Target[] targets,
            @Nonnull byte[] signature) {
        super(id, timestamp, pubKey, targets);
        this.signature = signature;
    }

    @Nonnull
    public static SignedTransaction fromTransaction(
            @Nonnull Transaction transaction,
            @Nonnull byte[] signature) {
        return new SignedTransaction(
                transaction.id,
                transaction.timestamp,
                transaction.pubKey,
                transaction.targets,
                signature);
    }

    public byte[] getSignature() {
        return signature;
    }
}
