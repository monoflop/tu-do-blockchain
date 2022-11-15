package com.philippkutsch.tuchain.jsonchain;

import javax.annotation.Nonnull;

public class Block {
    protected final long id;
    protected final byte[] prevHash;
    protected final BlockBody data;

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
}
