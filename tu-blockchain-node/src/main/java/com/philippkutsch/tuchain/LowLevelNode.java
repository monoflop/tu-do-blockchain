package com.philippkutsch.tuchain;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class LowLevelNode implements Network.Listener {
    private static final Logger logger
            = LoggerFactory.getLogger(LowLevelNode.class);

    private final ExecutorService executorService;
    protected final ListeningExecutorService service;

    protected final Config config;
    protected final RsaKeys rsaKeys;
    protected final Network network;

    public LowLevelNode(@Nonnull Config config, @Nonnull RsaKeys rsaKeys) {
        this.config = config;
        this.rsaKeys = rsaKeys;

        //Startup
        this.executorService = Executors.newCachedThreadPool();
        this.service = MoreExecutors.listeningDecorator(executorService);
        this.network = new Network(config.getName(), config.getPort(), service, this, config.getKnownPeers());
    }

    public void shutdown() throws IOException {
        this.network.shutdown();
        this.executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Shutdown failed");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Nonnull
    public Network getNetwork() {
        return network;
    }
}
