package com.philippkutsch.tuchain.blockchain;

import com.google.common.hash.HashCode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Blockchain {
    private final ExecutorService executorService;
    private final ListeningExecutorService service;

    private final List<HashedBlock> blockList;

    public Blockchain() {
        this.executorService = Executors.newCachedThreadPool();
        this.service = MoreExecutors.listeningDecorator(executorService);

        this.blockList = new ArrayList<>();
    }

    public void shutdown() {
        this.service.shutdown();
        this.executorService.shutdown();
    }

    @Nonnull
    public CompletableFuture<HashedBlock> generateGenesisBlock(
            @Nonnull byte[] data,
            @Nonnull HashMiningStepInterface hashInterface) {
        Block block = new Block(1, new byte[32], data.length, data);
        return calculateBlockHash(block, hashInterface);
    }

    @Nonnull
    public CompletableFuture<HashedBlock> generateBlock(
            @Nonnull HashedBlock prev,
            @Nonnull byte[] data,
            @Nonnull HashMiningStepInterface hashInterface) {
        Block block = new Block(prev.id + 1, prev.hash, data.length, data);
        return calculateBlockHash(block, hashInterface);
    }

    //Hashcash
    @Nonnull
    private CompletableFuture<HashedBlock> calculateBlockHash(
            @Nonnull Block block,
            @Nonnull HashMiningStepInterface hashInterface) {
        CompletableFuture<HashedBlock> future = new CompletableFuture<>();
        executorService.submit(() -> {
            //TODO Increase efficiency
            long nuOnce = 0;
            for(long i = 0; i < Integer.MAX_VALUE; i++) {
                long timestamp = System.currentTimeMillis();
                byte[] hash = block.generateHash(timestamp, nuOnce);

                hashInterface.onHashCreated(i, hash);

                String hashString = HashCode.fromBytes(hash).toString();
                String hashStringPrefix = hashString.substring(0, 6);
                //TODO handle hash size with BigInteger or something similar
                if(hashStringPrefix.equals("000000")) {
                    HashedBlock hashedBlock = new HashedBlock(block, timestamp, nuOnce, hash);
                    future.complete(hashedBlock);
                    break;
                }

                nuOnce++;
            }
        });

        return future;
    }

    public void addBlock(HashedBlock hashedBlock) {
        blockList.add(hashedBlock);
    }

    @Nullable
    public HashedBlock getLastBlock() {
        if(blockList.isEmpty()) {
            return null;
        }
        return blockList.get(blockList.size() - 1);
    }

    @Nonnull
    public List<HashedBlock> getBlockchain() {
        return blockList;
    }
}
