package com.philippkutsch.tuchain.chain;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

public class Block {
    protected final long id;
    protected final byte[] prevHash;
    protected final BlockBody data;

    @Nonnull
    public static Block generateGenesisBlock() {
        return new Block(1, new byte[]{}, new BlockBody(new Transaction[]{}));
    }

    public Block(long id,
                 @Nonnull byte[] prevHash,
                 @Nonnull BlockBody data) {
        this.id = id;
        this.prevHash = prevHash;
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public byte[] getPrevHash() {
        return prevHash;
    }

    public BlockBody getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return getId() == block.getId() && Arrays.equals(getPrevHash(), block.getPrevHash()) && getData().equals(block.getData());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getId(), getData());
        result = 31 * result + Arrays.hashCode(getPrevHash());
        return result;
    }
}
