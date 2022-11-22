package com.philippkutsch.tuchain.network.protocol;

import com.philippkutsch.tuchain.chain.HashedBlock;

import javax.annotation.Nonnull;

public class NewBlockMessage extends EncodeAbleMessage {
    public static final String TYPE = "nblock";

    private final HashedBlock hashedBlock;

    public NewBlockMessage(@Nonnull HashedBlock hashedBlock) {
        super(TYPE);
        this.hashedBlock = hashedBlock;
    }

    public HashedBlock getHashedBlock() {
        return hashedBlock;
    }
}
