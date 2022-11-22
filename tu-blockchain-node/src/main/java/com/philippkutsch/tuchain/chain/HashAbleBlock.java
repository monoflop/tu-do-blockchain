package com.philippkutsch.tuchain.chain;

import com.google.common.hash.Hashing;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;

public class HashAbleBlock extends Block {
    protected long timestamp;
    protected long nuOnce;

    public HashAbleBlock(
            long id,
            @Nonnull byte[] prevHash,
            @Nonnull BlockBody data) {
        super(id, prevHash, data);
    }

    @Nonnull
    public static HashAbleBlock fromBlock(@Nonnull Block block) {
        return new HashAbleBlock(block.id, block.prevHash, block.data);
    }

    @Nonnull
    public static HashAbleBlock fromHashedBlock(
            @Nonnull HashedBlock hashedBlock) {
        HashAbleBlock hashAbleBlock = new HashAbleBlock(hashedBlock.id, hashedBlock.prevHash, hashedBlock.data);
        hashAbleBlock.setTimestamp(hashedBlock.timestamp);
        hashAbleBlock.setNuOnce(hashedBlock.nuOnce);
        return hashAbleBlock;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setNuOnce(long nuOnce) {
        this.nuOnce = nuOnce;
    }

    @Nonnull
    public byte[] hash() {
        return Hashing.sha256()
                .hashBytes(ChainUtils.encodeToBytes(this))
                .asBytes();
    }
}