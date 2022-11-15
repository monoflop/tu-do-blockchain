package com.philippkutsch.tuchain.network.protocol.user.messages;

import com.philippkutsch.tuchain.network.protocol.EncodeAbleMessage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class BalanceMessage extends EncodeAbleMessage {
    public static final String TYPE = "balance";

    private final String publicKey;

    public BalanceMessage(@Nonnull String publicKey) {
        super(TYPE);
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }
}
