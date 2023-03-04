package com.philippkutsch.tuchain.chain;

import com.google.common.hash.Hashing;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

public class SignAbleContract {
    protected final long timestamp;
    protected final long deadline;
    protected final int goal;
    protected final byte[] ownerPubKey;

    //Meta data
    protected final String title;
    protected final String description;

    public SignAbleContract(
            long timestamp,
            long deadline,
            int goal,
            @Nonnull byte[] ownerPubKey,
            @Nonnull String title,
            @Nonnull String description) {
        this.timestamp = timestamp;
        this.deadline = deadline;
        this.goal = goal;
        this.ownerPubKey = ownerPubKey;
        this.title = title;
        this.description = description;
    }

    @Nonnull
    public Contract toContract(byte[] signature) {
        return new Contract(this, signature);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDeadline() {
        return deadline;
    }

    public int getGoal() {
        return goal;
    }

    public byte[] getOwnerPubKey() {
        return ownerPubKey;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignAbleContract that = (SignAbleContract) o;
        return getTimestamp() == that.getTimestamp() && getDeadline() == that.getDeadline() && getGoal() == that.getGoal() && Arrays.equals(getOwnerPubKey(), that.getOwnerPubKey()) && getTitle().equals(that.getTitle()) && getDescription().equals(that.getDescription());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getTimestamp(), getDeadline(), getGoal(), getTitle(), getDescription());
        result = 31 * result + Arrays.hashCode(getOwnerPubKey());
        return result;
    }
}
