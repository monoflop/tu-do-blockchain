package com.philippkutsch.tuchain.network;

import com.google.common.hash.HashCode;
import com.philippkutsch.tuchain.blockchain.HashMiningStepInterface;
import com.philippkutsch.tuchain.jsonchain.Block;
import com.philippkutsch.tuchain.jsonchain.HashAbleBlock;
import com.philippkutsch.tuchain.jsonchain.HashedBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Miner {
    private CompletableFuture<HashedBlock> resultFuture;
    private Future<?> miningFuture;

    @Nonnull
    public CompletableFuture<HashedBlock> startMining(
            @Nonnull ExecutorService executorService,
            @Nonnull Block block,
            @Nullable HashMiningStepInterface hashInterface) {
        //Check if mining is running and return running future
        if(isMining()) {
            return resultFuture;
        }

        //Start mining process
        CompletableFuture<HashedBlock> completableFuture = new CompletableFuture<>();
        miningFuture = executorService.submit(() -> {
            //TODO Increase efficiency
            HashAbleBlock hashAbleBlock = new HashAbleBlock(
                    block.getId(), block.getPrevHash(), block.getData());
            long nuOnce = 0;
            for(long i = 0; i < Long.MAX_VALUE; i++) {
                long timestamp = System.currentTimeMillis();
                hashAbleBlock.setTimestamp(timestamp);
                hashAbleBlock.setNuOnce(nuOnce);
                byte[] hash = hashAbleBlock.hash();

                if(hashInterface != null) {
                    hashInterface.onHashCreated(i, hash);
                }

                String hashString = HashCode.fromBytes(hash).toString();
                String hashStringPrefix = hashString.substring(0, 6);
                //TODO handle hash size with BigInteger or something similar
                if(hashStringPrefix.equals("000000")) {
                    HashedBlock hashedBlock = new HashedBlock(
                            block.getId(), block.getPrevHash(), block.getData(), timestamp, nuOnce, hash
                    );
                    completableFuture.complete(hashedBlock);
                    break;
                }

                if(Thread.interrupted()) {
                    //System.out.println("Mining interrupted");
                    break;
                }

                nuOnce++;
            }
        });
        resultFuture = completableFuture;
        return completableFuture;
    }

    public void stopMining() {
        if(!isMining()) {
            return;
        }

        miningFuture.cancel(true);
        resultFuture.cancel(true);
        miningFuture = null;
    }

    public boolean isMining() {
        return miningFuture != null && !miningFuture.isCancelled() && !miningFuture.isDone();//&& !miningFuture.isCompletedExceptionally()
    }
}

