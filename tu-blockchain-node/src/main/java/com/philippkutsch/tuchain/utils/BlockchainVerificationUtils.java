package com.philippkutsch.tuchain.utils;

import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.chain.Transaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class BlockchainVerificationUtils {
    private BlockchainVerificationUtils() {

    }

    @Nonnull
    public static VerificationResult validateBlockchain(
            @Nonnull Blockchain blockchain,
            int blockReward) {
        try {
            blockchain.beginReadAccess();

            //Empty chain is valid
            List<HashedBlock> copyList = blockchain.getBlockchain();
            if(copyList.isEmpty()) {
                return VerificationResult.success();
            }

            //Iterate over chain and validate blocks
            HashedBlock prevBlock = null;
            for (HashedBlock currentBlock : copyList) {
                //Genesis block only validate header
                if (prevBlock == null) {
                    if (!currentBlock.isHeaderValid()) {
                        return VerificationResult.error(
                                VerificationError.GenesisInvalidHeader,
                                currentBlock,
                                null);
                    }
                } else {
                    //Verify header
                    if (!currentBlock.isHeaderValid()) {
                        return VerificationResult.error(
                                VerificationError.InvalidHeader,
                                currentBlock,
                                null);
                    }

                    //Check block ids
                    if (prevBlock.getId() + 1 != currentBlock.getId()) {
                        return VerificationResult.error(
                                VerificationError.InvalidId,
                                currentBlock,
                                null);
                    }

                    //Check block hashes
                    if (!Arrays.equals(prevBlock.getHash(), currentBlock.getPrevHash())) {
                        return VerificationResult.error(
                                VerificationError.InvalidPrevHash,
                                currentBlock,
                                null);
                    }

                    //Check timestamp order
                    if (prevBlock.getTimestamp() >= currentBlock.getTimestamp()) {
                        return VerificationResult.error(
                                VerificationError.InvalidTimestampOrder,
                                currentBlock,
                                null);
                    }

                    //Verify transactions
                    Transaction[] transactions = currentBlock.getData().getTransactions();
                    Transaction prevTransaction = null;
                    for (int transactionId = 0; transactionId < transactions.length; transactionId++) {
                        //Coinbase
                        TransactionVerificationUtils.VerificationResult result;
                        if(transactionId == 0) {
                            result = TransactionVerificationUtils.verifyCoinbaseTransaction(
                                    transactions[transactionId], blockReward);
                        }
                        //Normal transaction
                        else {
                            result = TransactionVerificationUtils.verifyTransaction(
                                    blockchain, transactions[transactionId], false);
                        }
                        if (!result.isSuccess()) {
                            return VerificationResult.error(
                                    VerificationError.InvalidTransaction,
                                    currentBlock,
                                    result);
                        }

                        //Check timestamp order only for non coinbase transactions
                        if(prevTransaction != null && transactionId > 1) {
                            if(prevTransaction.getTimestamp() >= transactions[transactionId].getTimestamp()) {
                                return VerificationResult.error(
                                        VerificationError.InvalidTransactionTimestampOrder,
                                        currentBlock,
                                        null);
                            }
                        }

                        prevTransaction = transactions[transactionId];
                    }
                }

                prevBlock = currentBlock;
            }

            return VerificationResult.success();
        }
        finally {
            blockchain.endReadAccess();
        }
    }

    public enum VerificationError {
        GenesisInvalidHeader,
        InvalidHeader,
        InvalidId,
        InvalidPrevHash,
        InvalidTimestampOrder,
        InvalidTransaction,
        InvalidTransactionTimestampOrder
    }

    public static class VerificationResult {
        private final boolean success;
        private final VerificationError error;
        private final HashedBlock errorBlock;
        private final TransactionVerificationUtils.VerificationResult transactionError;

        @Nonnull
        public static VerificationResult success() {
            return new VerificationResult(true, null, null, null);
        }

        @Nonnull
        public static VerificationResult error(
                @Nonnull VerificationError error,
                @Nonnull HashedBlock errorBlock,
                @Nullable TransactionVerificationUtils.VerificationResult transactionError) {
            return new VerificationResult(false, error, errorBlock, transactionError);
        }

        public VerificationResult(
                boolean success,
                @Nullable VerificationError error,
                @Nullable HashedBlock errorBlock,
                @Nullable TransactionVerificationUtils.VerificationResult transactionError) {
            this.success = success;
            this.error = error;
            this.errorBlock = errorBlock;
            this.transactionError = transactionError;
        }

        public boolean isSuccess() {
            return success;
        }

        @Nullable
        public VerificationError getError() {
            return error;
        }

        @Nullable
        public HashedBlock getErrorBlock() {
            return errorBlock;
        }

        @Nullable
        public TransactionVerificationUtils.VerificationResult getTransactionError() {
            return transactionError;
        }

        @Nonnull
        public String errorString() {
            return "Blockchain verification error on block #" + (errorBlock == null ? "?" : errorBlock.getId()) + "\n" +
                    "Error: " + error + "\n" +
                    (transactionError == null ? ""
                            : "TransactionError: " + transactionError.getError());
        }
    }
}
