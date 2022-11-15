package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.philippkutsch.tuchain.jsonchain.Block;
import com.philippkutsch.tuchain.jsonchain.BlockBody;
import com.philippkutsch.tuchain.jsonchain.HashedBlock;
import com.philippkutsch.tuchain.jsonchain.SignedTransaction;
import com.philippkutsch.tuchain.jsonchain.utils.ChainUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Ignore
public class MinerTest {
    @Test
    public void mineGenesisBlockTest() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ListeningExecutorService service = MoreExecutors.listeningDecorator(executorService);

        BlockBody blockBody = new BlockBody(new SignedTransaction[]{});
        Block genesisBlock = new Block(1, new byte[0], blockBody);

        Miner miner = new Miner();
        CompletableFuture<HashedBlock> future = miner.startMining(executorService, genesisBlock, null);
        HashedBlock hashedBlock = future.get();
        System.out.println(ChainUtils.encodeToString(hashedBlock));

        executorService.shutdown();
        service.shutdown();
    }
}
