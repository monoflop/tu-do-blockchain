package com.philippkutsch.tuchain.chain;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class BlockBody {
    protected final SignedTransaction[] signedTransactions;

    public BlockBody(@Nonnull SignedTransaction[] signedTransactions) {
        this.signedTransactions = signedTransactions;
    }

    public SignedTransaction[] getSignedTransactions() {
        return signedTransactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockBody blockBody = (BlockBody) o;
        return Arrays.equals(getSignedTransactions(), blockBody.getSignedTransactions());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getSignedTransactions());
    }
}
