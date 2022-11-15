package com.philippkutsch.tuchain.network;

import com.philippkutsch.tuchain.App;
import com.philippkutsch.tuchain.jsonchain.HashedBlock;
import com.philippkutsch.tuchain.blockchain.RsaKeys;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.config.Node;
import com.philippkutsch.tuchain.jsonchain.utils.ChainUtils;
import com.philippkutsch.tuchain.network.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

public class Network {
    private static final Logger logger
            = LoggerFactory.getLogger(App.class);

    private final Config config;
    private final List<RemoteNode> networkNodeList;

    public Network(@Nonnull Config config) {
        this.config = config;
        this.networkNodeList = new ArrayList<>();

        List<HashedBlock> blockchainInitialState = new ArrayList<>();
        //Create blockchain from scratch
        if(config.getBlockchainFile() == null) {
            blockchainInitialState.add(ChainUtils.decodeFromString(
                    "{\"timestamp\":1667745075755,\"nuOnce\":24203771,\"hash\":\"AAAAj35/ExBxatM/3hNzZqt2mANd+g9/1j0GNvwEdL8\\u003d\",\"id\":1,\"prevHash\":\"\",\"data\":{\"signedTransactions\":[]}}", HashedBlock.class));
        }
        //Load blockchain
        //else {

        //}

        //Load nodes from config
        for(Node node : config.getNodeList()) {
            try {
                RsaKeys rsaKeys = RsaKeys.fromFiles(node.getPublicKeyFile(), node.getPrivateKeyFile());
                registerNode(node.getId(), rsaKeys, blockchainInitialState);
            }
            catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                logger.error("Failed to load node #" + node.getId());
                e.printStackTrace();
            }
        }
    }

    @Nonnull
    private RemoteNode registerNode(@Nonnull String id,
                                    @Nonnull RsaKeys rsaTool,
                                    @Nonnull List<HashedBlock> blockchainInitialState) {
        RemoteNode remoteNode = new TestingNode(this, id, rsaTool, blockchainInitialState);
        networkNodeList.add(remoteNode);
        return remoteNode;
    }

    //Send message to all network nodes
    public void broadcast(@Nonnull RemoteNode senderNode, @Nonnull Message message) {
        for(RemoteNode node : networkNodeList) {
            //Skip sender
            if(senderNode.getNodeId().equals(node.getNodeId())) {
                continue;
            }
            node.sendMessage(senderNode, message);
        }
    }

    public void sendUserMessage(@Nonnull String targetNodeId, @Nonnull Message message) {
        for(RemoteNode node : networkNodeList) {
            if(node.getNodeId().equals(targetNodeId)) {
                node.sendUserMessage(message);
            }
        }
    }

    public void shutdown() {
        for(RemoteNode node : networkNodeList) {
            node.shutdown();
        }
    }

    private void exportChain() {
        //Collect longest chain

    }
}
