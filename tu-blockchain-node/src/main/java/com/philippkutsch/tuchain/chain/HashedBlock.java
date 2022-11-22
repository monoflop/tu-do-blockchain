package com.philippkutsch.tuchain.chain;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

public class HashedBlock extends Block {
    protected final long timestamp;
    protected final long nuOnce;
    protected final byte[] hash;

    public HashedBlock(
            long id,
            @Nonnull byte[] prevHash,
            @Nonnull BlockBody data,
            long timestamp,
            long nuOnce,
            @Nonnull byte[] hash) {
        super(id, prevHash, data);
        this.timestamp = timestamp;
        this.nuOnce = nuOnce;
        this.hash = hash;
    }

    public boolean isHeaderValid() {
        HashAbleBlock hashAbleBlock = HashAbleBlock.fromHashedBlock(this);
        byte[] headerHash = hashAbleBlock.hash();
        return Arrays.equals(headerHash, hash);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getNuOnce() {
        return nuOnce;
    }

    public byte[] getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HashedBlock that = (HashedBlock) o;
        return getTimestamp() == that.getTimestamp() && getNuOnce() == that.getNuOnce() && Arrays.equals(getHash(), that.getHash());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), getTimestamp(), getNuOnce());
        result = 31 * result + Arrays.hashCode(getHash());
        return result;
    }
}
