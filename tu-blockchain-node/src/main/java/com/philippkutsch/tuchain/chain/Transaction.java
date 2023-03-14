package com.philippkutsch.tuchain.chain;

import com.google.common.hash.Hashing;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

public class Transaction {
    public static final transient byte[] COINBASE_TX_ID = new byte[32];
    public static final transient int COINBASE_V_OUT = 0;

    protected final long timestamp;

    protected final SignedInput[] inputs;

    private final Output[] outputs;

    @Nonnull
    public static Transaction buildCoinbaseTransaction(
            long timestamp,
            int blockReward,
            @Nonnull byte[] optionalData,
            @Nonnull byte[] targetPublicKey) {
        //32 null bytes since coinbase transaction has no input
        Transaction.Input input = new Transaction.Input(COINBASE_TX_ID, COINBASE_V_OUT);
        //Coinbase can have variable bytes as signature
        Transaction.SignedInput signedInput = new Transaction.SignedInput(input.getTxId(), input.getvOut(), optionalData);
        //Transaction output to miner public key
        Transaction.Output output = new Transaction.Output(blockReward, targetPublicKey);
        Transaction.SignedInput[] inputs = { signedInput };
        Transaction.Output[] outputs = { output };
        return new Transaction(timestamp, inputs, outputs);
    }

    public Transaction(long timestamp,
                       @Nonnull SignedInput[] inputs,
                       @Nonnull Output[] outputs) {
        this.timestamp = timestamp;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nonnull
    public SignedInput[] getInputs() {
        return inputs;
    }

    @Nonnull
    public Output[] getOutputs() {
        return outputs;
    }

    @Nonnull
    public SignAbleTransaction toSignAbleTransaction() {
        Input[] unSignedInputs = new Input[inputs.length];
        for(int i = 0; i < inputs.length; i++) {
            unSignedInputs[i] = inputs[i].toInput();
        }
        return new SignAbleTransaction(timestamp, unSignedInputs, outputs);
    }

    @Nonnull
    public byte[] getTransactionId() {
        byte[] transactionBytes = ChainUtils.encodeToBytes(this);
        return Hashing.sha256().hashBytes(transactionBytes).asBytes();
    }


    public boolean isContractFormatted() {
        boolean containsNoSignatures = true;
        for(Transaction.SignedInput input : inputs) {
            if(input.getSignature().length > 0) {
                containsNoSignatures = false;
                break;
            }
        }
        return containsNoSignatures && timestamp == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return getTimestamp() == that.getTimestamp() && Arrays.equals(getInputs(), that.getInputs()) && Arrays.equals(getOutputs(), that.getOutputs());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getTimestamp());
        result = 31 * result + Arrays.hashCode(getInputs());
        result = 31 * result + Arrays.hashCode(getOutputs());
        return result;
    }

    //To create a valid input and spent target outputs utxo.
    //You have to own the private key that correspont to the outputs public key
    public static class Input {
        //Input transactionId
        protected final byte[] txId;

        //Input transaction output index
        protected final int vOut;

        public Input(@Nonnull byte[] txId,
                     int vOut) {
            this.txId = txId;
            this.vOut = vOut;
        }

        public byte[] getTxId() {
            return txId;
        }

        public int getvOut() {
            return vOut;
        }

        @Nonnull
        public SignedInput toSignedInput(@Nonnull byte[] signature) {
            return new SignedInput(txId, vOut, signature);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Input input = (Input) o;
            return getvOut() == input.getvOut() && Arrays.equals(getTxId(), input.getTxId());
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(getvOut());
            result = 31 * result + Arrays.hashCode(getTxId());
            return result;
        }
    }

    //https://bitcoin.stackexchange.com/questions/45693/how-is-a-transactions-output-signed
    //https://developer.bitcoin.org/devguide/transactions.html
    //https://en.bitcoin.it/wiki/Transaction
    //https://developer.bitcoin.org/reference/transactions.html
    // Signature that unlocks output
    // Signature of all inputs and outputs without other input signatures.
    // Can be verified with referenced outputs public key
    public static class SignedInput extends Input {
        protected final byte[] signature;

        public SignedInput(@Nonnull byte[] txId,
                           int vOut,
                           @Nonnull
                           byte[] signature) {
            super(txId, vOut);
            this.signature = signature;
        }

        public byte[] getSignature() {
            return signature;
        }

        @Nonnull
        public Input toInput() {
            return new Input(txId, vOut);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            SignedInput that = (SignedInput) o;
            return Arrays.equals(getSignature(), that.getSignature());
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + Arrays.hashCode(getSignature());
            return result;
        }
    }

    //UTXO: To claim output, you have to own the private key, belonging to the outputs public key.
    public static class Output {
        //Amount that is spent
        protected final int amount;

        //PubKey
        protected final byte[] pubKey;

        public Output(int amount,
                      @Nonnull byte[] pubKey) {
            this.amount = amount;
            this.pubKey = pubKey;
        }

        public int getAmount() {
            return amount;
        }

        public byte[] getPubKey() {
            return pubKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Output output = (Output) o;
            return getAmount() == output.getAmount() && Arrays.equals(getPubKey(), output.getPubKey());
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(getAmount());
            result = 31 * result + Arrays.hashCode(getPubKey());
            return result;
        }
    }
}
