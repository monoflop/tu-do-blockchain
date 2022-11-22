package com.philippkutsch.tuchain.network.protocol;

import com.philippkutsch.tuchain.chain.Blockchain;

import javax.annotation.Nonnull;

public class BlockchainSyncMessage extends EncodeAbleMessage {
    public static final String TYPE = "bsync";
    private final Blockchain blockchain;

    public BlockchainSyncMessage(
            @Nonnull Blockchain blockchain) {
        super(TYPE);
        this.blockchain = blockchain;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }
}
