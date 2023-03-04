package com.philippkutsch.tuchain.network.protocol;

import com.philippkutsch.tuchain.chain.Contract;

import javax.annotation.Nonnull;

public class NewContractMessage extends EncodeAbleMessage {
    public static final String TYPE = "ncontr";

    private final Contract contract;

    public NewContractMessage(@Nonnull Contract contract) {
        super(TYPE);
        this.contract = contract;
    }

    @Nonnull
    public Contract getContract() {
        return contract;
    }
}