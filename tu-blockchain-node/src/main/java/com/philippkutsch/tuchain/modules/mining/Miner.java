package com.philippkutsch.tuchain.modules.mining;

import com.philippkutsch.tuchain.chain.Block;
import com.philippkutsch.tuchain.chain.HashAbleBlock;
import com.philippkutsch.tuchain.chain.HashedBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * Miner
 *
 * Customizable PoW calculation
 */
public class Miner implements Callable<HashedBlock> {
    private final int hashTargetBits;
    private final int maxHashPerSecond;
    private final Block block;
    private final HashMiningStepInterface hashInterface;

    public static int calculateZeroPrefix(byte[] hash) {
        int numberOfZeroBits = 0;
        for(byte hashByte : hash) {
            for (int bitIndex = 7; bitIndex >= 0; bitIndex--) {
                int bit = (hashByte >> bitIndex) & 1;
                if(bit == 1) {
                    return numberOfZeroBits;
                }
                numberOfZeroBits++;
            }
        }
        return numberOfZeroBits;
    }

    public Miner(int hashTargetBits,
                 int maxHashPerSecond,
                 @Nonnull Block block,
                 @Nullable HashMiningStepInterface hashInterface) {
        this.hashTargetBits = hashTargetBits;
        this.maxHashPerSecond = maxHashPerSecond;
        this.block = block;
        this.hashInterface = hashInterface;
    }

    @Override
    public HashedBlock call() throws Exception {
        HashAbleBlock hashAbleBlock = new HashAbleBlock(
                block.getId(), block.getPrevHash(), block.getData());
        long nuOnce = 0;
        for (long i = 0; i < Long.MAX_VALUE; i++) {
            long timestamp = System.currentTimeMillis();
            hashAbleBlock.setTimestamp(timestamp);
            hashAbleBlock.setNuOnce(nuOnce);
            byte[] hash = hashAbleBlock.hash();

            if (hashInterface != null) {
                hashInterface.onHashCreated(i, hash);
            }

            //Iterate over byte array and count number of unset bits
            int numberOfZeroBits = calculateZeroPrefix(hash);

            //Difficulty target met
            if (numberOfZeroBits == hashTargetBits) {
                return new HashedBlock(
                        block.getId(), block.getPrevHash(), block.getData(), timestamp, nuOnce, hash);
            }

            if (Thread.interrupted()) {
                break;
            }

            nuOnce++;

            //Rate limit hashing for testing purpose
            //Basically we can assume hashing is done instantly (very low overhead)
            if(maxHashPerSecond > 0) {
                Thread.sleep(1000 / maxHashPerSecond);
            }
        }

        throw new Exception("Failed to hash block");
    }
}
