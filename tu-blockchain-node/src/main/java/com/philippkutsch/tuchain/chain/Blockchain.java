package com.philippkutsch.tuchain.chain;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Blockchain {
    private transient final ReadWriteLock lock;

    private final List<HashedBlock> blockList;

    //Default constructor for gson only, because gson does not initialise the lock otherwise
    public Blockchain() {
        this.blockList = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock(true);
    }

    public Blockchain(@Nonnull List<HashedBlock> initialState) {
        if(initialState.size() == 0) {
            throw new IllegalStateException("Expected blockchain size of at least one");
        }

        this.blockList = new ArrayList<>(initialState);
        this.lock = new ReentrantReadWriteLock(true);
    }

    public void addBlock(@Nonnull HashedBlock hashedBlock) {
        try {
            lock.writeLock().lock();
            blockList.add(hashedBlock);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Nonnull
    public HashedBlock getLastBlock() {
        try {
            lock.readLock().lock();
            return blockList.get(blockList.size() - 1);
        } finally {
            lock.readLock().unlock();
        }
    }

    //Dangerous methods
    public void beginReadAccess() {
        lock.readLock().lock();
    }
    public void endReadAccess() {
        lock.readLock().unlock();
    }
    public void beginWriteAccess() {
        lock.writeLock().lock();
    }
    public void endWriteAccess() {
        lock.writeLock().unlock();
    }

    //Return a copy of the blockchain
    @Nonnull
    public List<HashedBlock> getBlockchain() {
        try {
            lock.readLock().lock();
            return new ArrayList<>(blockList);
        } finally {
            lock.readLock().unlock();
        }
    }

    //List contracts
    @Nonnull
    public List<Contract> listContracts() {
        List<Contract> contractList = new ArrayList<>();
        List<HashedBlock> blockListCopy = getBlockchain();
        for(HashedBlock block : blockListCopy) {
            contractList.addAll(Arrays.asList(block.getData().getContracts()));
        }
        return contractList;
    }

    //Search for a contract
    @Nonnull
    public Optional<Contract> findContract(@Nonnull byte[] contractId) {
        List<HashedBlock> blockListCopy = getBlockchain();
        for(HashedBlock block : blockListCopy) {
            for(Contract contract : block.getData().getContracts()) {
                if(Arrays.equals(contract.getContractId(), contractId)) {
                    return Optional.of(contract);
                }
            }
        }
        return Optional.empty();
    }

    //Search for a transaction
    @Nonnull
    public Optional<Transaction> findTransaction(@Nonnull byte[] transactionId) {
        List<HashedBlock> blockListCopy = getBlockchain();
        for(HashedBlock block : blockListCopy) {
            for(Transaction transaction : block.getData().getTransactions()) {
                if(Arrays.equals(transaction.getTransactionId(), transactionId)) {
                    return Optional.of(transaction);
                }
            }
        }
        return Optional.empty();
    }

    @Nonnull
    private List<Transaction.Input> findTransactionInputs(
            @Nonnull List<HashedBlock> blockListCopy,
            @Nonnull byte[] txId) {
        List<Transaction.Input> inputList = new ArrayList<>();
        for(HashedBlock block : blockListCopy) {
            for(Transaction transaction : block.getData().getTransactions()) {
                for(Transaction.Input input : transaction.getInputs()) {
                    if(Arrays.equals(input.getTxId(), txId)) {
                        inputList.add(input);
                    }
                }
            }
        }
        return inputList;
    }

    @Nonnull
    public Optional<Transaction.Input> findTransactionInput(
            @Nonnull byte[] txId,
            int vOut) {
        List<HashedBlock> blockListCopy = getBlockchain();
        for(HashedBlock block : blockListCopy) {
            for(Transaction transaction : block.getData().getTransactions()) {
                for(Transaction.Input input : transaction.getInputs()) {
                    if(Arrays.equals(input.getTxId(), txId) && input.getvOut() == vOut) {
                        return Optional.of(input);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Nonnull
    public List<Transaction> findUTXOTransaction(@Nonnull byte[] pubKey) {
        List<HashedBlock> blockListCopy = getBlockchain();
        List<Transaction> uTXOTransactionList = new ArrayList<>();
        //Iterate over all transaction outputs and check if pubKey is matching
        for(HashedBlock hashedBlock : blockListCopy) {
            for(Transaction transaction : hashedBlock.getData().getTransactions()) {
                Transaction.Output[] outputs = transaction.getOutputs();
                for(int i = 0; i < outputs.length; i++) {
                    if(Arrays.equals(pubKey, outputs[i].getPubKey())) {
                        //Find inputs that are referencing this output
                        byte[] txId = transaction.getTransactionId();
                        List<Transaction.Input> inputList =
                                findTransactionInputs(blockListCopy, txId);

                        //Check if vOut is referenced
                        boolean spend = false;
                        for(Transaction.Input input : inputList) {
                            if(input.getvOut() == i) {
                                //If we have a reference, the transaction is spent
                                spend = true;
                                break;
                            }
                        }

                        if(!spend) {
                            uTXOTransactionList.add(transaction);
                        }
                    }
                }
            }
        }
        return uTXOTransactionList;
    }

    @Nonnull
    public List<UnspentTransactionOutput> findUTXO(@Nonnull byte[] pubKey) {
        List<UnspentTransactionOutput> uTXOList = new ArrayList<>();
        List<Transaction> uTXOTransactions = findUTXOTransaction(pubKey);
        for(Transaction transaction : uTXOTransactions) {
            Transaction.Output[] outputs = transaction.getOutputs();
            for(int i = 0; i < outputs.length; i++) {
                if(Arrays.equals(outputs[i].getPubKey(), pubKey)) {
                    uTXOList.add(new UnspentTransactionOutput(
                            transaction.getTransactionId(), i, outputs[i].getAmount()));
                }
            }
        }
        return uTXOList;
    }

    @Nonnull
    public Block buildNextBlock(
            @Nonnull List<Transaction> transactionList,
            @Nonnull List<Contract> contractList) {
        HashedBlock currentBlock = getLastBlock();
        BlockBody blockBody = new BlockBody(
                transactionList.toArray(new Transaction[0]),
                contractList.toArray(new Contract[0]));
        return new Block(currentBlock.id + 1, currentBlock.hash, blockBody);
    }
}
