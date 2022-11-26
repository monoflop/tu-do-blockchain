package com.philippkutsch.tuchain.chain;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Blockchain {
    private static transient final Logger logger
            = LoggerFactory.getLogger(Blockchain.class);

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
    public void beginWriteAccess() {
        lock.writeLock().lock();
    }
    public void endWriteAccess() {
        lock.writeLock().unlock();
    }

    //Return a copy of the blockchain
    @Nonnull
    public List<HashedBlock> getBlockchain() {
        return new ArrayList<>(blockList);
    }



    @Nonnull
    public Block buildNextBlock(@Nonnull List<SignedTransaction> transactionList) {
        HashedBlock currentBlock = getLastBlock();
        BlockBody blockBody = new BlockBody(transactionList.toArray(new SignedTransaction[0]));
        return new Block(currentBlock.id + 1, currentBlock.hash, blockBody);
    }

    //TODO Think there is an issue with block validation
    //  Not all blocks are validated
    public boolean validateChain() {
        List<HashedBlock> copyList = new ArrayList<>(blockList);
        if(copyList.isEmpty()) {
            return true;
        }

        if(copyList.size() == 1) {
            HashedBlock genesisBlock = copyList.get(0);
            return genesisBlock.isHeaderValid();
        }
        else {
            HashedBlock prevBlock = copyList.get(0);
            for(int i = 1; i < copyList.size(); i++) {
                HashedBlock currentBlock = copyList.get(i);

                //Check header (signature is correct)
                if(!prevBlock.isHeaderValid()) {
                    logger.warn("Block validation: header invalid #" + prevBlock.getId());
                    return false;
                }

                //Check if blocks are chained correctly
                if(prevBlock.getId() + 1 != currentBlock.getId()) {
                    logger.warn("Block validation: id invalid #" + prevBlock.getId()
                            + " #" + currentBlock.getId());
                    return false;
                }

                //Check if block hashes matches
                if(!Arrays.equals(prevBlock.hash, currentBlock.prevHash)) {
                    logger.warn("Block validation: hash mismatch #" + prevBlock.getId()
                            + " #" + currentBlock.getId());
                    return false;
                }

                try {
                    //TODO check transactions
                    for(SignedTransaction signedTransaction
                            : prevBlock.getData().getSignedTransactions()) {
                        boolean valid = signedTransaction.isValid();
                        if(!valid) {
                            logger.warn("Block validation: block #" + prevBlock.getId()
                                    + " transaction #" + signedTransaction.getId() + " invalid signature");
                            return false;
                        }
                    }
                }
                catch (NoSuchAlgorithmException | InvalidKeySpecException |
                        InvalidKeyException | SignatureException e) {
                    logger.warn("Block validation: block # " + prevBlock.getId()
                            + " invalid signature exception", e);
                    return false;
                }

                prevBlock = currentBlock;
            }
        }

        return true;
    }
}
