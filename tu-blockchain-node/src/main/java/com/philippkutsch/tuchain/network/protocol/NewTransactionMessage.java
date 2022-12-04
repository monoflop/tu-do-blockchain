package com.philippkutsch.tuchain.network.protocol;

import com.philippkutsch.tuchain.chain.Transaction;

import javax.annotation.Nonnull;

public class NewTransactionMessage extends EncodeAbleMessage {
    public static final String TYPE = "ntrans";

    private final Transaction transaction;

    public NewTransactionMessage(@Nonnull Transaction transaction) {
        super(TYPE);
        this.transaction = transaction;
    }

    @Nonnull
    public Transaction getTransaction() {
        return transaction;
    }
}