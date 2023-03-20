package com.philippkutsch.tuchain.utils;

import com.philippkutsch.tuchain.RsaKeys;
import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.chain.Transaction;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class for transaction verification
 */
public class TransactionVerificationUtils {
    private TransactionVerificationUtils() {

    }

    @Nonnull
    public static VerificationResult verifyCoinbaseTransaction(
            @Nonnull Transaction transaction,
            int coinbaseReward) {
        //Check if we have one input
        if(transaction.getInputs().length != 1) {
            return VerificationResult.error(VerificationError.CoinbaseInvalidInputCount);
        }

        Transaction.SignedInput coinbaseInput = transaction.getInputs()[0];

        //Check if we have all null txId
        if(!Arrays.equals(coinbaseInput.getTxId(), Transaction.COINBASE_TX_ID)) {
            return VerificationResult.error(VerificationError.CoinbaseInvalidTxId);
        }

        //Check if we have vOut = 0
        if(coinbaseInput.getvOut() != Transaction.COINBASE_V_OUT) {
            return VerificationResult.error(VerificationError.CoinbaseInvalidVOut);
        }

        //Signature is not important for coinbase transactions

        //Check if we have one output
        if(transaction.getOutputs().length != 1) {
            return VerificationResult.error(VerificationError.CoinbaseInvalidOutputCount);
        }

        Transaction.Output coinbaseOutput = transaction.getOutputs()[0];

        //Check reward
        if(coinbaseOutput.getAmount() != coinbaseReward) {
            return VerificationResult.error(VerificationError.CoinbaseInvalidReward);
        }

        //Check pupKey
        try {
            new RsaKeys(coinbaseOutput.getPubKey(), null);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return VerificationResult.error(VerificationError.InvalidOutputPubKey);
        }

        return VerificationResult.success();
    }

    @Nonnull
    public static VerificationResult verifyTransaction(
            @Nonnull Blockchain blockchain,
            @Nonnull Transaction transaction,
            boolean newTransaction) {
        try {
            blockchain.beginReadAccess();

            //Check if we have at least one input
            if(transaction.getInputs().length == 0) {
                return VerificationResult.error(VerificationError.InvalidInputCount);
            }

            //Verify inputs and signature
            int inputSum = 0;
            byte[] transactionBytes = ChainUtils
                    .encodeToBytes(transaction.toSignAbleTransaction());
            for(Transaction.SignedInput input : transaction.getInputs()) {
                //Find target transaction and output
                Optional<Transaction> transactionOptional =
                        blockchain.findTransaction(input.getTxId());
                if(transactionOptional.isEmpty()) {
                    return VerificationResult.error(VerificationError.InvalidInputTxId);
                }

                //Check if transaction vOut is present
                Transaction targetTransaction = transactionOptional.get();
                if(input.getvOut() > targetTransaction.getOutputs().length - 1) {
                    return VerificationResult.error(VerificationError.InvalidInputVOut);
                }

                //Check if transaction is already spent
                if(newTransaction) {
                    Optional<Transaction.Input> referencingInput = blockchain
                            .findTransactionInput(targetTransaction.getTransactionId(), input.getvOut());
                    if(referencingInput.isPresent()) {
                        return VerificationResult.error(VerificationError.InvalidAlreadySpent);
                    }
                }

                //Load target output pubKey
                Transaction.Output targetOutput = targetTransaction.getOutputs()[input.getvOut()];
                RsaKeys outputKey;
                try {
                    outputKey = new RsaKeys(targetOutput.getPubKey(), null);
                }
                catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    return VerificationResult.error(VerificationError.InvalidTargetOutputPubKey);
                }

                //Check signature
                try {
                    boolean success = outputKey.verifyData(transactionBytes, input.getSignature());
                    if(!success) {
                        return VerificationResult.error(VerificationError.InvalidInputSignature);
                    }
                }
                catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    return VerificationResult.error(VerificationError.InvalidInputSignatureFormat);
                }

                //Sum inputs
                inputSum += targetOutput.getAmount();
            }

            //Check if we have at least one output
            if(transaction.getOutputs().length == 0) {
                return VerificationResult.error(VerificationError.InvalidOutputCount);
            }

            //Check outputs pubKeys and sum amount
            int outputSum = 0;
            for(Transaction.Output output : transaction.getOutputs()) {
                outputSum += output.getAmount();
                //Do not verify pubKey because contract addresses are not valid pubKeys
            }

            //Check if sum of inputs = sum of outputs
            if(inputSum != outputSum) {
                return VerificationResult.error(VerificationError.InvalidInputOutputSum);
            }

            return VerificationResult.success();
        }
        finally {
            blockchain.endReadAccess();
        }
    }

    public enum VerificationError {
        CoinbaseInvalidInputCount,
        CoinbaseInvalidTxId,
        CoinbaseInvalidVOut,
        CoinbaseInvalidOutputCount,
        CoinbaseInvalidReward,
        InvalidInputCount,
        InvalidOutputPubKey,
        InvalidOutputCount,
        InvalidInputTxId,
        InvalidInputVOut,
        InvalidAlreadySpent,
        InvalidTargetOutputPubKey,
        InvalidInputSignatureFormat,
        InvalidInputSignature,
        InvalidInputOutputSum
    }

    public static class VerificationResult {
        private final boolean success;
        private final VerificationError error;

        @Nonnull
        public static VerificationResult success() {
            return new VerificationResult(true, null);
        }

        @Nonnull
        public static VerificationResult error(@Nonnull VerificationError error) {
            return new VerificationResult(false, error);
        }

        public VerificationResult(
                boolean success,
                @Nullable VerificationError error) {
            this.success = success;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        @Nullable
        public VerificationError getError() {
            return error;
        }
    }
}
