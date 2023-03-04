package com.philippkutsch.tuchain.chain;

import com.google.common.hash.Hashing;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;

public class Contract extends SignAbleContract {
    public byte[] signature;

    public Contract(long timestamp,
                    long deadline,
                    int goal,
                    @Nonnull byte[] ownerPubKey,
                    @Nonnull String title,
                    @Nonnull String description,
                    @Nonnull byte[] signature) {
        super(timestamp, deadline, goal, ownerPubKey, title, description);
        this.signature = signature;
    }

    public Contract(@Nonnull SignAbleContract signAbleContract, @Nonnull byte[] signature) {
        super(signAbleContract.timestamp,
                signAbleContract.deadline,
                signAbleContract.goal,
                signAbleContract.ownerPubKey,
                signAbleContract.title,
                signAbleContract.description);
        this.signature = signature;
    }

    @Nonnull
    public SignAbleContract toSignAbleContract() {
        return this;
    }

    @Nonnull
    public byte[] getContractId() {
        byte[] contractBytes = ChainUtils.encodeToBytes(this);
        return Hashing.sha256().hashBytes(contractBytes).asBytes();
    }
}
