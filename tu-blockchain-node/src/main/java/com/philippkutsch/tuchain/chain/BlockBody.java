package com.philippkutsch.tuchain.chain;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class BlockBody {
    protected final Transaction[] transactions;

    public BlockBody(@Nonnull Transaction[] transactions) {
        this.transactions = transactions;
    }

    @Nonnull
    public Transaction[] getTransactions() {
        return transactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockBody blockBody = (BlockBody) o;
        return Arrays.equals(getTransactions(), blockBody.getTransactions());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getTransactions());
    }
}
