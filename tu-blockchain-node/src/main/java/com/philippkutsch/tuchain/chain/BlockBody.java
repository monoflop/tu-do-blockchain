package com.philippkutsch.tuchain.chain;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class BlockBody {
    protected final Transaction[] transactions;
    protected final Contract[] contracts;

    public BlockBody(@Nonnull Transaction[] transactions,
                     @Nonnull Contract[] contracts) {
        this.transactions = transactions;
        this.contracts = contracts;
    }

    @Nonnull
    public Transaction[] getTransactions() {
        return transactions;
    }

    @Nonnull
    public Contract[] getContracts() {
        return contracts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockBody blockBody = (BlockBody) o;
        return Arrays.equals(getTransactions(), blockBody.getTransactions()) && Arrays.equals(getContracts(), blockBody.getContracts());
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(getTransactions());
        result = 31 * result + Arrays.hashCode(getContracts());
        return result;
    }
}
