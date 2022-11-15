package com.philippkutsch.tuchain.network.protocol.network.messages;

import com.philippkutsch.tuchain.jsonchain.HashedBlock;
import com.philippkutsch.tuchain.network.protocol.EncodeAbleMessage;

import javax.annotation.Nonnull;

public class NewBlockMessage extends EncodeAbleMessage {
    public static final String TYPE = "newblock";

    private final HashedBlock block;

    public NewBlockMessage(@Nonnull HashedBlock block) {
        super(TYPE);
        this.block = block;
    }

    public HashedBlock getBlock() {
        return block;
    }
}
