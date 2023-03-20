package com.philippkutsch.tuchain;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.modules.NodeModule;
import com.philippkutsch.tuchain.network.Network;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Abstract node implementation.
 *
 * Allows to extend basic functionality with modules
 *
 * LowLevel handler for:
 * - Executor service
 * - Config
 * - RsaKeys (Loaded from config)
 * - Blockchain
 * - Network
 */
public abstract class Node implements Network.Listener {
    private static transient final Logger logger
            = LoggerFactory.getLogger(Node.class);

    private final ExecutorService executorService;
    protected final ListeningExecutorService service;

    protected final Config config;
    protected final RsaKeys rsaKeys;
    protected final Blockchain blockchain;
    protected Network network;

    private final List<NodeModule> moduleList;

    public Node(@Nonnull Config config,
                @Nonnull RsaKeys rsaKeys,
                @Nonnull Blockchain blockchain)
            throws IOException {
        this.config = config;
        this.rsaKeys = rsaKeys;
        this.blockchain = blockchain;
        this.moduleList = new ArrayList<>();

        //Startup
        this.executorService = Executors.newCachedThreadPool();
        this.service = MoreExecutors.listeningDecorator(executorService);
    }

    //We put network setup into run, so all modules have a chance to register
    protected void run() throws IOException {
        this.network = new Network(config.getName(), config.getPort(), service, this, config.getKnownPeers());
    }

    @Override
    public void onMessage(
            @Nonnull RemoteNode remoteNode,
            @Nonnull Message message) {
        boolean handled = false;
        for(NodeModule module : moduleList) {
            handled = module.onMessage(remoteNode, message);
            if(handled) {
                break;
            }
        }

        if(!handled) {
            logger.warn("Message " + message.getType() + " unhandled");
        }
    }

    @Override
    public void onNodeConnected(@Nonnull RemoteNode remoteNode) {
        for(NodeModule module : moduleList) {
            module.onNodeConnected(remoteNode);
        }
    }

    @Nullable
    public <T extends NodeModule> T getModule(Class<T> type)
            throws RuntimeException {
        for(NodeModule module : moduleList) {
            if(module.getClass() == type) {
                return type.cast(module);
            }
        }
        return null;
    }

    @Nonnull
    public <T extends NodeModule> T requireModule(Class<T> type)
            throws RuntimeException {
        T module = getModule(type);
        if(module == null) {
            throw new RuntimeException("Module not found " + type.getSimpleName());
        }
        return module;
    }

    public void registerModule(@Nonnull NodeModule module) {
        moduleList.add(module);
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

    @Nonnull
    public Blockchain getBlockchain() {
        return blockchain;
    }

    @Nonnull
    public Config getConfig() {
        return config;
    }

    @Nonnull
    public ListeningExecutorService getService() {
        return service;
    }

    @Nonnull
    public RsaKeys getRsaKeys() {
        return rsaKeys;
    }
}
