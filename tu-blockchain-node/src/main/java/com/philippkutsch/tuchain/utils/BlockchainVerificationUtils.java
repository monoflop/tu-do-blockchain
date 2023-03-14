package com.philippkutsch.tuchain.utils;

import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.chain.Contract;
import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.chain.Transaction;
import com.philippkutsch.tuchain.contract.ContractException;
import com.philippkutsch.tuchain.contract.ContractVm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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

                    //unpack contract outgoing transactions
                    //collect contract incoming transactions
                    List<Transaction> transactionList = new ArrayList<>(Arrays.asList(currentBlock.getData().getTransactions()));
                    List<Transaction> contractInvokingTransactions = new ArrayList<>();
                    List<Transaction> contractGeneratedTransactions = new ArrayList<>();
                    Iterator<Transaction> transactionIterator = transactionList.iterator();
                    while (transactionIterator.hasNext()) {
                        Transaction transaction = transactionIterator.next();
                        if(transaction.getInputs().length == 1
                                && transaction.getOutputs().length >= 1
                                && transaction.getOutputs()[0].getPubKey().length == 32) {
                            //Search contract
                            byte[] contractAddress = transaction.getOutputs()[0].getPubKey();
                            Optional<Contract> contractOptional = blockchain.findContract(contractAddress);
                            if (contractOptional.isEmpty()) {
                                continue;
                            }

                            contractInvokingTransactions.add(transaction);
                        }
                        else if(transaction.isContractFormatted()) {
                            contractGeneratedTransactions.add(transaction);
                            transactionIterator.remove();
                        }
                    }

                    //Verify transactions
                    Transaction prevTransaction = null;
                    for (int transactionId = 0; transactionId < transactionList.size(); transactionId++) {
                        //Coinbase
                        TransactionVerificationUtils.VerificationResult result;
                        if(transactionId == 0) {
                            result = TransactionVerificationUtils.verifyCoinbaseTransaction(
                                    transactionList.get(transactionId), blockReward);
                        }
                        //Normal transaction
                        else {
                            result = TransactionVerificationUtils.verifyTransaction(
                                    blockchain, transactionList.get(transactionId), false);
                        }
                        if (!result.isSuccess()) {
                            return VerificationResult.error(
                                    VerificationError.InvalidTransaction,
                                    currentBlock,
                                    result);
                        }

                        //Check timestamp order only for non coinbase transactions
                        if(prevTransaction != null && transactionId > 1) {
                            if(prevTransaction.getTimestamp() >= transactionList.get(transactionId).getTimestamp()) {
                                return VerificationResult.error(
                                        VerificationError.InvalidTransactionTimestampOrder,
                                        currentBlock,
                                        null);
                            }
                        }

                        prevTransaction = transactionList.get(transactionId);
                    }

                    //Verify contract transactions
                    for(Transaction transaction : contractInvokingTransactions) {
                        //Search contract
                        byte[] contractAddress = transaction.getOutputs()[0].getPubKey();
                        Optional<Contract> contractOptional = blockchain.findContract(contractAddress);
                        if(contractOptional.isEmpty()) {
                            continue;
                        }

                        try {
                            //Run contract in context of blockchain before current block
                            List<HashedBlock> blockchainCopy = blockchain.getBlockchain();
                            List<HashedBlock> chain = blockchainCopy.subList(0, blockchainCopy.indexOf(currentBlock));
                            Blockchain subChain = new Blockchain(chain);

                            //Run contract
                            List<Transaction> contractResult = ContractVm
                                    .run(contractOptional.get(), transaction, 0, subChain);

                            //Check if output is available in contractGeneratedTransactions
                            if(contractResult.size() > 0) {
                                //Remove transaction from contractGeneratedTransactions if present
                                for(Transaction created : contractResult) {
                                    boolean found = false;
                                    for(Transaction present : contractGeneratedTransactions) {
                                        if(Arrays.equals(created.getTransactionId(), present.getTransactionId())) {
                                            found = true;
                                            contractGeneratedTransactions.remove(present);
                                            break;
                                        }
                                    }
                                    //If generated transactions are not inside contractGeneratedTransactions, fail
                                    if(!found) {
                                        return VerificationResult.error(VerificationError.InvalidContractResult, currentBlock, null);
                                    }
                                }
                            }
                        }
                        catch (ContractException e) {}
                    }

                    //Check if there are some transactions remaining
                    if(contractGeneratedTransactions.size() > 0) {
                        return VerificationResult.error(VerificationError.InvalidContractTransactions, currentBlock, null);
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
        InvalidTransactionTimestampOrder,
        InvalidContractResult,
        InvalidContractTransactions
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
