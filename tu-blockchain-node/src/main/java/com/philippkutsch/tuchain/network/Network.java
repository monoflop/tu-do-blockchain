package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.philippkutsch.tuchain.config.Peer;
import com.philippkutsch.tuchain.network.protocol.Message;
import com.philippkutsch.tuchain.network.protocol.network.HandshakeMessage;
import com.philippkutsch.tuchain.network.protocol.network.NewNodeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

//TODO fix handshake and connection issue
public class Network implements NetworkServer.Listener {
    private static final Logger logger
            = LoggerFactory.getLogger(Network.class);

    private final String name;
    private final int listeningPort;
    private final ListeningExecutorService service;
    private final Listener listener;
    private final NetworkServer networkServer;
    private final ListenableFuture<?> networkListeningFuture;
    private final List<RemoteNode> remoteNodeList;
    private final List<RemoteNode> pendingNodeList;
    private final Semaphore remoteNodeListLock;

    public Network(@Nonnull String name,
                   int listeningPort,
                   @Nonnull ListeningExecutorService service,
                   @Nonnull Listener listener,
                   @Nonnull List<Peer> initialPeers) {
        this.name = name;
        this.listeningPort = listeningPort;
        this.service = service;
        this.listener = listener;
        this.networkServer = new NetworkServer(listeningPort, this);
        this.remoteNodeList = new ArrayList<>();
        this.pendingNodeList = new ArrayList<>();
        this.remoteNodeListLock = new Semaphore(1, true);

        //Start listening
        networkListeningFuture = service.submit(() -> {
            try {
                networkServer.startListen();
            }
            catch (IOException e) {
                logger.error("Listening failed", e);
            }
        });

        //Try to connect to initial peers
        for(Peer peer : initialPeers) {
            service.submit(() -> {
                logger.debug("Trying to connect well known peer " + peer.getIp() + ":" + peer.getPort());
                tryConnect(peer.getIp(), peer.getPort());
            });
        }
    }

    public void broadcast(@Nonnull Message message, @Nullable RemoteNode ignoreNode) {
        for(RemoteNode node : remoteNodeList) {
            if(ignoreNode != null && ignoreNode.getName().equals(node.getName())) {
                continue;
            }

            if(node.isAuthenticated()) {
                node.send(message);
            }
        }
    }

    public void shutdown() throws IOException {
        networkListeningFuture.cancel(true);
        for(RemoteNode node : pendingNodeList) {
            node.shutdown();
        }
        for(RemoteNode node : remoteNodeList) {
            node.shutdown();
        }
    }

    public void tryConnect(String host, int port) {
        Optional<Socket> socketOptional = RemoteNode.tryConnect(host, port);
        if(socketOptional.isPresent()) {
            RemoteNode node = new RemoteNode(
                    socketOptional.get(),
                    this::onUnauthenticatedNodeMessage,
                    this::onUnauthenticatedNodeDisconnected);
            service.submit(node);
            pendingNodeList.add(node);
        }
    }

    @Override
    public void onSocketConnected(@Nonnull Socket socket) {
        RemoteNode node = new RemoteNode(
                socket,
                this::onUnauthenticatedNodeMessage,
                this::onUnauthenticatedNodeDisconnected);
        service.submit(node);
        pendingNodeList.add(node);

        //Send handshake message
        service.submit(() -> {
            List<Peer> knownPeers = new ArrayList<>();
            for(RemoteNode knownNode : remoteNodeList) {
                knownPeers.add(new Peer(knownNode.getConnection().getAddress(), knownNode.getListenPort()));
            }
            HandshakeMessage handshakeMessage = new HandshakeMessage(1, name, listeningPort, knownPeers);
            node.send(handshakeMessage.encode());
        });
    }

    public void onUnauthenticatedNodeMessage(@Nonnull RemoteNode remoteNode, @Nonnull Message message) {
        //Only handle handshake
        if(HandshakeMessage.TYPE.equals(message.getType())) {
            HandshakeMessage handshakeMessage = message.to(HandshakeMessage.class);
            remoteNode.setName(handshakeMessage.getName());
            remoteNode.setListenPort(handshakeMessage.getPort());
            remoteNode.setAuthenticated(true);

            //Check if the node is already in remoteNodeList
            if(isAlreadyConnected(handshakeMessage.getName())) {
                logger.debug("Remote node '" + remoteNode.getName() + "' already connected");
                try {
                    remoteNode.shutdown();
                }
                catch (IOException e) {
                    logger.error("Node disconnect failed", e);
                }
                return;
            }

            logger.debug("Remote node '" + remoteNode.getName() + "' connected");
            remoteNode.updateListener(this::onNodeMessage, this::onNodeDisconnected);

            pendingNodeList.removeIf(node -> node.getName().equals(remoteNode.getName()));
            remoteNodeList.add(remoteNode);

            //TODO connect to all unknown nodes send with handshake
            Iterator<Peer> peerIterator = handshakeMessage.getKnownPeers().iterator();
            while (peerIterator.hasNext()) {
                Peer peer = peerIterator.next();
                for(RemoteNode existingNode : remoteNodeList) {
                    if(existingNode.getConnection().getAddress().equals(peer.getIp())
                            && existingNode.getListenPort() == peer.getPort()) {
                        peerIterator.remove();
                    }
                }

                //Remove self TODO correct bound address
                if(peer.getIp().equals("127.0.0.1") && peer.getPort() == listeningPort) {
                    peerIterator.remove();
                }
            }
            logger.debug("Received " + handshakeMessage.getKnownPeers().size() + " unconnected network nodes");
            for(Peer peer : handshakeMessage.getKnownPeers()) {
                logger.debug("Try connect to " + peer.getIp() + ":" + peer.getPort());
                tryConnect(peer.getIp(), peer.getPort());
            }

            //Respond with handshake
            List<Peer> knownPeers = new ArrayList<>();
            for(RemoteNode knownNode : remoteNodeList) {
                knownPeers.add(new Peer(knownNode.getConnection().getAddress(), knownNode.getListenPort()));
            }
            HandshakeMessage responseHandshakeMessage = new HandshakeMessage(1, name, listeningPort, knownPeers);
            remoteNode.send(responseHandshakeMessage.encode());

            //Broadcast new node to the network
            NewNodeMessage newNodeMessage = new NewNodeMessage(
                    handshakeMessage.getName(),
                    remoteNode.getConnection().getAddress(),
                    handshakeMessage.getPort());
            broadcast(newNodeMessage.encode(), remoteNode);
        }
    }

    public void onNodeMessage(@Nonnull RemoteNode remoteNode, @Nonnull Message message) {
        if(NewNodeMessage.TYPE.equals(message.getType())) {
            NewNodeMessage newNodeMessage = message.to(NewNodeMessage.class);

            //Check if we already know the node
            for(RemoteNode node : remoteNodeList) {
                if(newNodeMessage.getName().equals(node.getName()) /*|| (
                        //Ip and port
                        node.getConnection().getPort() == newNodeMessage.getPort()
                                && node.getConnection().getAddress().equals(newNodeMessage.getHost())
                        )*/) {
                    logger.debug("Node " + node.getName() + " already known");
                    return;
                }
            }

            //Try connect
            tryConnect(newNodeMessage.getHost(), newNodeMessage.getPort());

            //Rebroadcast node to network
            //broadcast(message, remoteNode);

           /* try {
                remoteNodeListLock.acquire();
                try {

                } finally {
                    remoteNodeListLock.release();
                }
            }
            catch (InterruptedException e) {
                logger.error("Lock error", e);
            }*/
        }
        else {
            listener.onMessage(remoteNode, message);
        }
    }

    private boolean isAlreadyConnected(@Nonnull String name) {
        for(RemoteNode node : remoteNodeList) {
            if(name.equals(node.getName())) {
                return true;
            }
        }
        return false;
    }

    public void onNodeDisconnected(@Nonnull RemoteNode remoteNode) {
        remoteNodeList.removeIf(node -> node.getName().equals(remoteNode.getName()));
    }

    public void onUnauthenticatedNodeDisconnected(@Nonnull RemoteNode remoteNode) {
        remoteNodeList.removeIf(node -> node.getName().equals(remoteNode.getName()));
    }

    @Nonnull
    public List<RemoteNode> getNodes() {
        return new ArrayList<>(remoteNodeList);
    }

    @Nonnull
    public List<RemoteNode> getPendingNodeList() {
        return pendingNodeList;
    }

    public interface Listener {
        void onMessage(@Nonnull RemoteNode remoteNode, @Nonnull Message message);
    }
}
