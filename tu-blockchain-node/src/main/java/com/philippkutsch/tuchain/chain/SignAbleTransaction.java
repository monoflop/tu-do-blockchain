package com.philippkutsch.tuchain.chain;

import javax.annotation.Nonnull;

public class SignAbleTransaction {
    protected final long timestamp;

    protected final Transaction.Input[] inputs;

    private final Transaction.Output[] outputs;

    public SignAbleTransaction(
            long timestamp,
            @Nonnull Transaction.Input[] inputs,
            @Nonnull Transaction.Output[] outputs) {
        this.timestamp = timestamp;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nonnull
    public Transaction.Input[] getInputs() {
        return inputs;
    }

    @Nonnull
    public Transaction.Output[] getOutputs() {
        return outputs;
    }
}
