package com.philippkutsch.tuchain.network.protocol.user.messages;

import com.philippkutsch.tuchain.network.protocol.EncodeAbleMessage;

import javax.annotation.Nonnull;

public class PublishTransactionMessage extends EncodeAbleMessage {
    public static final String TYPE = "publish-transaction";

    private final String sourceWallet;
    private final String targetWallet;
    private final int amount;
    private final String signature;

    public PublishTransactionMessage(@Nonnull String sourceWallet,
                                     @Nonnull String targetWallet,
                                     int amount,
                                     @Nonnull String signature) {
        super(TYPE);
        this.sourceWallet = sourceWallet;
        this.targetWallet = targetWallet;
        this.amount = amount;
        this.signature = signature;
    }

    public String getSourceWallet() {
        return sourceWallet;
    }

    public String getTargetWallet() {
        return targetWallet;
    }

    public int getAmount() {
        return amount;
    }

    public String getSignature() {
        return signature;
    }
}
