package com.philippkutsch.tuchain.contract;

import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.chain.Contract;
import com.philippkutsch.tuchain.chain.Transaction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the SCVM
 */
public class ContractVm {
    @Nonnull
    public static byte[] findSendingPubKey(
            @Nonnull Blockchain blockchain,
            @Nonnull Transaction transaction) {
        //Find target public key
        //Limitation: contract transaction work only with one input and one output
        byte[] inputTransactionId = transaction.getInputs()[0].getTxId();
        Optional<Transaction> transactionOptional = blockchain.findTransaction(inputTransactionId);
        if(transactionOptional.isEmpty()) {
            throw new IllegalStateException("Referencing transaction not found");
        }
        Transaction referencingTransaction = transactionOptional.get();
        return referencingTransaction.getOutputs()[transaction.getInputs()[0].getvOut()].getPubKey();
    }

    @Nonnull
    public static List<Transaction> findInvestorIncomingTransactions(
            @Nonnull Blockchain blockchain,
            @Nonnull List<Transaction> investmentTransactions,
            @Nonnull byte[] investorPublicKey) {
        List<Transaction> investorInvestments = new ArrayList<>();
        for(Transaction investment : investmentTransactions) {
            byte[] investmentPublicKey = findSendingPubKey(blockchain, investment);
            if(Arrays.equals(investmentPublicKey, investorPublicKey)) {
                investorInvestments.add(investment);
            }
        }
        return investorInvestments;
    }

    @Nonnull
    public static List<Transaction> findInvestorOutgoingTransactions(
            @Nonnull Blockchain blockchain,
            @Nonnull List<Transaction> incomingTransactions) {
        List<Transaction> outgoingTransactions = new ArrayList<>();
        //Check if incoming transactions are spent
        for(Transaction transaction : incomingTransactions) {
            Optional<Transaction> referenceOptional = blockchain
                    .findReferencingTransaction(transaction.getTransactionId());
            if(referenceOptional.isPresent()) {
                outgoingTransactions.add(referenceOptional.get());
            }
        }
        return outgoingTransactions;
    }

    @Nonnull
    public static Optional<Transaction> findOwnerOutgoingTransaction(
            @Nonnull Contract contract,
            @Nonnull Blockchain blockchain,
            @Nonnull byte[] ownerPublicKey,
            int investmentSum) {
        List<Transaction> transactionsToOwner = blockchain.findTransactionsTo(ownerPublicKey);
        transactionsToOwner.removeIf(transaction -> transaction.getTimestamp() != 0 || transaction.getOutputs().length != 1);
        for(Transaction transaction : transactionsToOwner) {
            boolean validInputs = true;
            for(Transaction.SignedInput input : transaction.getInputs()) {
                Optional<Transaction> reference = blockchain.findTransaction(input.getTxId());
                if(reference.isEmpty()
                        || !Arrays.equals(reference.get().getOutputs()[input.getvOut()].getPubKey(), contract.getContractId())) {
                    validInputs = false;
                    break;
                }
            }

            if(!validInputs) {
                continue;
            }

            //must be the contract investment sum
            if(transaction.getOutputs()[0].getAmount() == investmentSum) {
                return Optional.of(transaction);
            }
        }

        return Optional.empty();
    }

    @Nonnull
    public static List<Transaction> run(
            @Nonnull Contract contract,
            @Nonnull Transaction transaction,
            int vOut,
            @Nonnull Blockchain blockchain)
            throws ProjectRunningException,
            ProjectSuccessOwnerOnlyException,
            NotAInvestorException,
            AlreadyWithdrawnException {
        List<Transaction> generatedTransactionList = new ArrayList<>();

        //Decide contract method DEPOSIT / WITHDRAW
        int transactionAmount = transaction.getOutputs()[vOut].getAmount();
        //WITHDRAW
        if(transactionAmount == 0) {
            //Check if project is still running
            if(transaction.getTimestamp() <= contract.getDeadline()) {
                throw new ProjectRunningException();
            }

            //Check if project is successful
            //Collect transactions that happened to the contract before the deadline
            List<Transaction> investmentTransactions = blockchain.findTransactionsTo(contract.getContractId());
            investmentTransactions.removeIf((trans) -> trans.getTimestamp() > contract.getDeadline());

            //Sum transactions
            //TODO: can be simplified because every contract transaction has only one input and one output expect contract issued transactions
            int investmentSum = investmentTransactions.stream().mapToInt(t -> {
                int outputSum = 0;
                for(Transaction.Output output : t.getOutputs()) {
                    if(Arrays.equals(output.getPubKey(), contract.getContractId())) {
                        outputSum += output.getAmount();
                    }
                }
                return outputSum;
            }).sum();

            //Investor
            byte[] targetPubKey = findSendingPubKey(blockchain, transaction);

            //Collect contract incoming transactions
            List<Transaction> incomingTransactions = findInvestorIncomingTransactions(
                    blockchain, investmentTransactions, targetPubKey);

            List<Transaction> outgoingTransactions = findInvestorOutgoingTransactions(
                    blockchain, incomingTransactions);

            //Project was successful
            if(investmentSum >= contract.getGoal()) {
                //Only owner can withdraw
                if(!Arrays.equals(contract.getOwnerPubKey(), targetPubKey)) {
                    throw new ProjectSuccessOwnerOnlyException();
                }

                //Find owner withdraw transaction
                Optional<Transaction> optionalPayout = findOwnerOutgoingTransaction(contract, blockchain, targetPubKey, investmentSum);
                if(optionalPayout.isPresent()) {
                    throw new AlreadyWithdrawnException();
                }

                //Create transaction with all investing transaction as inputs
                List<Transaction.SignedInput> signedInputList = new ArrayList<>();
                for(Transaction investment : investmentTransactions) {
                    Transaction.SignedInput input = new Transaction.SignedInput(investment.getTransactionId(), 0, new byte[0]);
                    signedInputList.add(input);
                }

                //Transaction.SignedInput input = new Transaction.SignedInput(transaction.getTransactionId(), vOut, new byte[0]);
                Transaction.Output output = new Transaction.Output(investmentSum, targetPubKey);
                Transaction returnTransaction = new Transaction(0, signedInputList.toArray(new Transaction.SignedInput[0]), new Transaction.Output[]{output});
                generatedTransactionList.add(returnTransaction);
                return generatedTransactionList;
            }
            //Investment failed
            //Every investor can withdraw
            else {
                //Check if the user is even a investor
                if(incomingTransactions.isEmpty()) {
                    throw new NotAInvestorException();
                }

                if(outgoingTransactions.size() > 0) {
                    throw new AlreadyWithdrawnException();
                }

                //Create one transaction back, with input of all transactions
                List<Transaction.SignedInput> signedInputList = new ArrayList<>();
                int investorInvestmentSum = 0;
                for(Transaction investment : incomingTransactions) {
                    Transaction.SignedInput input = new Transaction.SignedInput(investment.getTransactionId(), 0, new byte[0]);
                    signedInputList.add(input);
                    investorInvestmentSum += investment.getOutputs()[0].getAmount();
                }

                Transaction.Output output = new Transaction.Output(investorInvestmentSum, targetPubKey);
                Transaction returnTransaction = new Transaction(0, signedInputList.toArray(new Transaction.SignedInput[0]), new Transaction.Output[]{output});
                generatedTransactionList.add(returnTransaction);
                return generatedTransactionList;
            }
        }
        //DEPOSIT
        else {
            //Project is still running so do nothing
            if(transaction.getTimestamp() <= contract.getDeadline()) {
                return generatedTransactionList;
            }

            //Project already ended, so return investment
            //Find target public key
            byte[] targetPubKey = findSendingPubKey(blockchain, transaction);

            //Create transaction back to investor
            Transaction.SignedInput input = new Transaction.SignedInput(transaction.getTransactionId(), vOut, new byte[0]);
            Transaction.Output output = new Transaction.Output(transaction.getOutputs()[vOut].getAmount(), targetPubKey);
            Transaction returnTransaction = new Transaction(0, new Transaction.SignedInput[]{input}, new Transaction.Output[]{output});
            generatedTransactionList.add(returnTransaction);
            return generatedTransactionList;
        }
    }
}
