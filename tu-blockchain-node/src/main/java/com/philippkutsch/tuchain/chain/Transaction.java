package com.philippkutsch.tuchain.chain;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

public class Transaction {
    protected final long id;
    protected final long timestamp;

    protected final byte[] pubKey;

    protected final Target[] targets;

    public Transaction(
            long id,
            long timestamp,
            @Nonnull byte[] pubKey,
            @Nonnull Target[] targets) {
        this.id = id;
        this.timestamp = timestamp;
        this.pubKey = pubKey;
        this.targets = targets;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public Target[] getTargets() {
        return targets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return getId() == that.getId() && getTimestamp() == that.getTimestamp() && Arrays.equals(getPubKey(), that.getPubKey()) && Arrays.equals(getTargets(), that.getTargets());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getId(), getTimestamp());
        result = 31 * result + Arrays.hashCode(getPubKey());
        result = 31 * result + Arrays.hashCode(getTargets());
        return result;
    }

    public static class Target {
        protected final int amount;
        protected final byte[] pubKey;

        public Target(int amount, @Nonnull byte[] pubKey) {
            this.amount = amount;
            this.pubKey = pubKey;
        }

        public int getAmount() {
            return amount;
        }

        public byte[] getPubKey() {
            return pubKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Target target = (Target) o;
            return getAmount() == target.getAmount() && Arrays.equals(getPubKey(), target.getPubKey());
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(getAmount());
            result = 31 * result + Arrays.hashCode(getPubKey());
            return result;
        }
    }
}
