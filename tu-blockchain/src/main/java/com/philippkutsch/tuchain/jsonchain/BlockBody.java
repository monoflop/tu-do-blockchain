package com.philippkutsch.tuchain.jsonchain;

import javax.annotation.Nonnull;

public class BlockBody {
    protected final SignedTransaction[] signedTransactions;

    public BlockBody(@Nonnull SignedTransaction[] signedTransactions) {
        this.signedTransactions = signedTransactions;
    }

    public SignedTransaction[] getSignedTransactions() {
        return signedTransactions;
    }
}
