package com.philippkutsch.tuchain;

import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.modules.BlockchainSyncModule;
import com.philippkutsch.tuchain.modules.mining.MiningModule;
import com.philippkutsch.tuchain.modules.ModuleLoadException;
import com.philippkutsch.tuchain.modules.PingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

//Local implementation
public class TestingNode extends Node {
    private static final Logger logger
            = LoggerFactory.getLogger(TestingNode.class);

    public TestingNode(@Nonnull Config config,
                       @Nonnull RsaKeys rsaKeys,
                       @Nonnull Blockchain blockchain)
            throws IOException {
        super(config, rsaKeys, blockchain);
        try {
            registerModule(new PingModule(this));
            registerModule(new MiningModule(this));
            registerModule(new BlockchainSyncModule(this));
            run();
        }
        catch (ModuleLoadException e) {
            logger.error("Failed to load modules", e);
            shutdown();
        }
    }
}
