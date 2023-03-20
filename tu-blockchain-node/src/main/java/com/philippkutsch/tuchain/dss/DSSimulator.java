package com.philippkutsch.tuchain.dss;

import com.google.common.util.concurrent.*;
import com.philippkutsch.tuchain.chain.*;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.modules.mining.Miner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DSSimulator
 *
 * Very simple simulation of a double spending attack
 */
public class DSSimulator {
    private static final String INITIAL_BLOCKCHAIN = "{\"blockList\":[{\"timestamp\":1677938378295,\"nuOnce\":78901825,\"hash\":\"AAAAzJDi3v8OCKv8qAevudm8/KnGRDz5dtnamXEOA88\",\"id\":1,\"prevHash\":\"\",\"data\":{\"transactions\":[],\"contracts\":[]}}]}";

    private final File outputFile;
    private final int maxBlocks;
    private final int minerDifficulty;
    private final int honestHps;
    private final int attackerHps;

    public DSSimulator(@Nonnull File outputFile,
                       int maxBlocks,
                       int minerDifficulty,
                       int honestHps,
                       int attackerHps) {
        this.outputFile = outputFile;
        this.maxBlocks = maxBlocks;
        this.minerDifficulty = minerDifficulty;
        this.honestHps = honestHps;
        this.attackerHps = attackerHps;
    }

    void simulate() throws IOException, InterruptedException {
        //Output
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"time", "block", "type"});
        CsvHelper csvHelper = new CsvHelper(outputFile);

        //Async stuff
        ExecutorService executorService = Executors.newCachedThreadPool();
        ListeningExecutorService service = MoreExecutors.listeningDecorator(executorService);

        //Initial blockchain
        Blockchain blockchainHonestNode = ChainUtils.decodeFromString(
                INITIAL_BLOCKCHAIN, Blockchain.class);
        Blockchain blockchainAttackerNode = ChainUtils.decodeFromString(
                INITIAL_BLOCKCHAIN, Blockchain.class);

        final long simulationStart = System.currentTimeMillis();

        data.add(new String[]{"" + 0, "1", "Verteidiger"});
        data.add(new String[]{"" + 0, "1", "Angreifer"});

        final NodeSimulator honestNode = new NodeSimulator(service, blockchainHonestNode, minerDifficulty, honestHps);
        final NodeSimulator attackerNode = new NodeSimulator(service, blockchainAttackerNode, minerDifficulty, attackerHps);
        honestNode.start((block) -> {
            System.out.println("Honest node created next block #" + block.getId());
            data.add(new String[]{"" + (System.currentTimeMillis() - simulationStart), "" + block.getId(), "Verteidiger"});
            if(block.getId() >= maxBlocks) {
                honestNode.stop();
                attackerNode.stop();
                service.shutdownNow();
                return true;
            }
            return false;
        });
        attackerNode.start((block) -> {
            System.out.println("Attacker node created next block #" + block.getId());
            data.add(new String[]{"" + (System.currentTimeMillis() - simulationStart), "" + block.getId(), "Angreifer"});
            if(block.getId() >= maxBlocks) {
                honestNode.stop();
                attackerNode.stop();
                service.shutdownNow();
                return true;
            }
            return false;
        });

        service.awaitTermination(60, TimeUnit.MINUTES);

        data.add(new String[]{"" + (System.currentTimeMillis() - simulationStart), "" + honestNode.getBlockchain().getLastBlock().getId(), "Verteidiger"});
        data.add(new String[]{"" + (System.currentTimeMillis() - simulationStart), "" + attackerNode.getBlockchain().getLastBlock().getId(), "Angreifer"});
        csvHelper.writeData(data);
    }

    private static class NodeSimulator implements FutureCallback<HashedBlock> {
        private final ListeningExecutorService service;
        private final Blockchain blockchain;
        private final int minerDifficulty;
        private final int minerHps;
        private OnBlockCreatedListener listener;
        private ListenableFuture<HashedBlock> future;

        public NodeSimulator(@Nonnull ListeningExecutorService service,
                             @Nonnull Blockchain blockchain,
                             int minerDifficulty,
                             int minerHps) {
            this.service = service;
            this.blockchain = blockchain;
            this.minerDifficulty = minerDifficulty;
            this.minerHps = minerHps;
        }

        public void start(@Nonnull OnBlockCreatedListener listener) {
            this.listener = listener;

            HashedBlock prevBlock = blockchain.getLastBlock();
            Block transactionBlock = new Block(prevBlock.getId() + 1, prevBlock.getHash(), new BlockBody(new Transaction[]{}, new Contract[]{}));
            Miner miner = new Miner(minerDifficulty, minerHps, transactionBlock, null);
            future = service.submit(miner);
            Futures.addCallback(future, this, service);
        }

        public void stop() {
            future.cancel(true);
        }

        public Blockchain getBlockchain() {
            return blockchain;
        }

        @Override
        public void onSuccess(@Nullable HashedBlock hashedBlock) {
            if(hashedBlock == null) {
                return;
            }

            blockchain.addBlock(hashedBlock);
            boolean stop = listener.onBlockCreated(hashedBlock);
            if(stop) {
                return;
            }

            //Submit next block
            Block nextBlock = new Block(hashedBlock.getId() + 1, hashedBlock.getHash(), new BlockBody(new Transaction[]{}, new Contract[]{}));
            Miner miner = new Miner(minerDifficulty, minerHps, nextBlock, null);
            future = service.submit(miner);
            Futures.addCallback(future, this, service);
        }

        @Override
        public void onFailure(@Nonnull Throwable throwable) {
            System.out.println("Failed to create block " + throwable);
        }

        public interface OnBlockCreatedListener {
            boolean onBlockCreated(@Nonnull HashedBlock hashedBlock);
        }
    }
}
