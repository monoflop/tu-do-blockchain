package com.philippkutsch.tuchain.jsonchain;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class HashedBlock extends Block{
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
}
