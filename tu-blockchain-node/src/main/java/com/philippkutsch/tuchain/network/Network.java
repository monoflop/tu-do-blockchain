package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.config.Peer;
import com.philippkutsch.tuchain.network.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Network implements NodeManager.Listener {
    private static final Logger logger
            = LoggerFactory.getLogger(Network.class);

    private final Listener listener;
    private final NodeManager nodeManager;
    private final Map<String, RemoteNode> nodeMap;
    private final Map<String, Receiver> listenMap;

    public Network(
            @Nonnull String name,
            int listeningPort,
            @Nonnull ListeningExecutorService service,
            @Nonnull Listener listener,
            @Nonnull List<Peer> initialPeers)
            throws IOException {
        this.listener = listener;
        this.nodeManager = new NodeManager(service, name, "127.0.0.1", listeningPort, initialPeers, this);
        this.nodeMap = new ConcurrentHashMap<>();
        this.listenMap = new ConcurrentHashMap<>();
    }

    public void broadcast(
            @Nonnull Message message,
            @Nonnull RemoteNode... exclude) {
        for(Map.Entry<String, RemoteNode> entry : nodeMap.entrySet()) {
            boolean skip = false;
            for(RemoteNode excludeNode : exclude) {
                if(entry.getKey().equals(excludeNode.getKey())) {
                    skip = true;
                    break;
                }
            }

            if(!skip) {
                send(entry.getValue(), message);
            }
        }
    }

    public void send(@Nonnull RemoteNode node, @Nonnull Message message) {
        logger.debug("Sending message to " + node.getKey() + " " + ChainUtils.encodeToString(message));
        nodeManager.send(node.getConnectedNode(), message);
    }

    @Nonnull
    public CompletableFuture<Message> sendAndReceive(
            @Nonnull RemoteNode node,
            @Nonnull Message message,
            @Nonnull String expectedType) {
        CompletableFuture<Message> completableFuture = new CompletableFuture<>();
        Receiver receiver = new Receiver(expectedType, completableFuture);
        listenMap.put(node.getKey(), receiver);
        send(node, message);
        return completableFuture;
    }

    @Nonnull
    public List<RemoteNode> getConnectedNodes() {
        List<RemoteNode> connectedNodeList = new ArrayList<>();
        for(Map.Entry<String, RemoteNode> entry : nodeMap.entrySet()) {
            connectedNodeList.add(entry.getValue());
        }
        return connectedNodeList;
    }

    public void shutdown() throws IOException {
        nodeManager.shutdown();
    }

    @Override
    public void onConnected(@Nonnull ConnectedNode connectedNode) {
        RemoteNode remoteNode = new RemoteNode(connectedNode);
        nodeMap.put(remoteNode.getKey(), remoteNode);
        logger.debug("Remote node connected " + remoteNode.getKey());
        listener.onNodeConnected(remoteNode);
    }

    @Override
    public void onDisconnected(@Nonnull ConnectedNode connectedNode) {
        nodeMap.remove(connectedNode.getKey());
        logger.debug("Remote node disconnected " + connectedNode.getKey());
    }

    @Override
    public void onMessage(@Nonnull ConnectedNode connectedNode, @Nonnull Message message) {
        RemoteNode node = nodeMap.get(connectedNode.getKey());
        if(node != null) {

            //Check if message node is in listen map
            if(listenMap.containsKey(connectedNode.getKey())) {
                //Check if type is valid
                Receiver receiver = listenMap.get(connectedNode.getKey());
                if(receiver.type.equals(message.getType())) {
                    receiver.future.complete(message);
                    listenMap.remove(connectedNode.getKey());
                    return;
                }
            }

            //Forward message
            listener.onMessage(node, message);
        }
    }

    public interface Listener {
        void onMessage(@Nonnull RemoteNode remoteNode, @Nonnull Message message);
        default void onNodeConnected(@Nonnull RemoteNode remoteNode) {

        }
    }

    private static class Receiver {
        private final String type;
        private final CompletableFuture<Message> future;

        public Receiver(@Nonnull String type,
                        @Nonnull CompletableFuture<Message> future) {
            this.type = type;
            this.future = future;
        }
    }
}