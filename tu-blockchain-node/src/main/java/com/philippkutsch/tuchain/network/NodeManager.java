package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.config.Peer;
import com.philippkutsch.tuchain.network.protocol.Message;
import com.philippkutsch.tuchain.network.protocol.network.HandshakeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle RemoteNode connection and protocol
 *
 * Pass successfully connected RemoteNodes to listener
 */
public class NodeManager implements ConnectionManager.Listener {
    private static final Logger logger
            = LoggerFactory.getLogger(NodeManager.class);

    private final HandshakeMessage handshakeMessage;
    private final String host;
    private final int port;
    private final ConnectionManager connectionManager;
    private final Listener listener;

    private final Map<String, PendingNode> pendingNodes;
    private final Map<String, ConnectedNode> connectedNodes;

    public NodeManager(
            @Nonnull ListeningExecutorService service,
            @Nonnull String name,
            @Nonnull String host,
            int listeningPort,
            @Nonnull List<Peer> knownPeers,
            @Nonnull Listener listener)
            throws IOException {
        this.handshakeMessage = new HandshakeMessage(1, name, host, listeningPort, knownPeers);
        this.host = host;
        this.port = listeningPort;
        this.connectionManager = new ConnectionManager(service, listeningPort, this);
        this.listener = listener;
        this.pendingNodes = new ConcurrentHashMap<>();
        this.connectedNodes = new ConcurrentHashMap<>();

        //Try to connect to known peers
        for(Peer knownPeer : knownPeers) {
            connectionManager.connect(knownPeer.getIp(), knownPeer.getPort());
        }
    }

    @Override
    public void onConnected(boolean incoming, @Nonnull NetworkConnection connection) {
        logger.debug("onConnected " + connection.getKey());
        pendingNodes.put(connection.getKey(), new PendingNode(incoming, connection));

        //If outgoing, start by sending handshake message
        if(!incoming) {
            connectionManager.send(connection, handshakeMessage.encode());
        }
    }

    @Override
    public void onDisconnected(@Nonnull NetworkConnection connection) {
        logger.debug("onDisconnected " + connection.getKey());
        pendingNodes.remove(connection.getKey());

        //Check if connection is already authorized
        Optional<ConnectedNode> connectedNodeOptional
                = connectedNodeFromConnection(connection);
        if(connectedNodeOptional.isPresent()) {
            ConnectedNode node = connectedNodeOptional.get();
            listener.onDisconnected(node);
            logger.debug("Node " + node.getKey() + " disconnected");
            connectedNodes.remove(node.getKey());
        }
    }

    @Override
    public void onMessage(@Nonnull NetworkConnection connection, @Nonnull Message message) {
        logger.debug("onMessage "  + connection.getKey() + " " + ChainUtils.encodeToString(message));
        if(HandshakeMessage.TYPE.equals(message.getType())) {
            HandshakeMessage remoteHandshake = message.to(HandshakeMessage.class);

            if(!pendingNodes.containsKey(connection.getKey())) {
                logger.debug("Received handshake even if node is not pending");
                return;
            }

            PendingNode pendingNode = pendingNodes.get(connection.getKey());
            //Send back handshake message for incoming nodes
            if(pendingNode.incoming) {
                connectionManager.send(connection, handshakeMessage.encode());
            }

            //Check if we already know this node peer
            //TODO Lock this part with semaphore
            ConnectedNode connectedNode = new ConnectedNode(connection, remoteHandshake.getName(),
                    remoteHandshake.getHost(), remoteHandshake.getPort(), pendingNode.incoming);
            if(connectedNodes.containsKey(connectedNode.getKey())) {
                logger.debug("Node " + connectedNode.getKey() + " is already connected");
                connectionManager.disconnect(connection);
                return;
            }
            pendingNodes.remove(connection.getKey());
            connectedNodes.put(connectedNode.getKey(), connectedNode);

            //Add node to knownPeers if not already stored
            boolean found = false;
            for(Peer peer : handshakeMessage.getKnownPeers()) {
                if(peer.getIp().equals(connectedNode.getHost()) && peer.getPort() == connectedNode.getPort()) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                handshakeMessage.getKnownPeers().add(new Peer(connectedNode.getHost(), connectedNode.getPort()));
            }

            //For small networks we need no advertising
            //Advertise new node to the network
            /*Message newNodeMessage = new NewNodeMessage(connectedNode.getName(),
                    connectedNode.getHost(), connectedNode.getPort()).encode();
            for(Map.Entry<String, ConnectedNode> node : connectedNodes.entrySet()) {
                if(!node.getKey().equals(connectedNode.getKey())) {
                    logger.debug("Advertising node " + connectedNode.getKey() + " to " + node.getKey());
                    connectionManager.send(node.getValue().getConnection(), newNodeMessage);
                }
            }*/

            listener.onConnected(connectedNode);

            logger.debug("Node " + connectedNode.getKey() + " connected");

            //Check known peers and try to connect
            List<Peer> knownPeerList = new ArrayList<>(remoteHandshake.getKnownPeers());
            for(Peer peer : knownPeerList) {
                logger.debug("Received peer " + peer.getIp() + ":" + peer.getPort());
            }

            Iterator<Peer> newPeersIterator = knownPeerList.iterator();
            while (newPeersIterator.hasNext()) {
                Peer newPeer = newPeersIterator.next();
                //its us
                if(newPeer.getIp().equals(host) && newPeer.getPort() == port) {
                    newPeersIterator.remove();
                    continue;
                }

                //Check our connected peers
                for(Map.Entry<String, ConnectedNode> node : connectedNodes.entrySet()) {
                    if(node.getValue().getHost().equals(newPeer.getIp())
                            && node.getValue().getPort() == newPeer.getPort()) {
                        newPeersIterator.remove();
                    }
                }
            }

            logger.debug("Node " + connectedNode.getKey() + " send " + knownPeerList.size() + " new unknown peers");
            for(Peer newPeer : knownPeerList) {
                connectionManager.connect(newPeer.getIp(), newPeer.getPort());
            }
        }
        else {
            //Check if message was from connectedNode
            Optional<ConnectedNode> connectedNodeOptional = connectedNodeFromConnection(connection);
            if(connectedNodeOptional.isEmpty()) {
                logger.warn("Connection " + connection.getKey() + " send message without authorization");
                return;
            }

            ConnectedNode connectedNode = connectedNodeOptional.get();
            listener.onMessage(connectedNode, message);
        }
    }

    public void shutdown() throws IOException {
        pendingNodes.clear();
        connectionManager.shutdown();
    }

    public void send(@Nonnull ConnectedNode connectedNode, @Nonnull Message message) {
        connectionManager.send(connectedNode.getConnection(), message);
    }

    private Optional<ConnectedNode> connectedNodeFromConnection(
            @Nonnull NetworkConnection connection) {
        for(Map.Entry<String, ConnectedNode> node : connectedNodes.entrySet()) {
            if(node.getValue().getConnection().getKey().equals(connection.getKey())) {
                return Optional.of(node.getValue());
            }
        }
        return Optional.empty();
    }

    public interface Listener {
        void onConnected(@Nonnull ConnectedNode connectedNode);
        void onDisconnected(@Nonnull ConnectedNode connectedNode);
        void onMessage(@Nonnull ConnectedNode connectedNode, @Nonnull Message message);
    }

    private static class PendingNode {
        private final boolean incoming;
        private final NetworkConnection connection;

        public PendingNode(boolean incoming, @Nonnull NetworkConnection connection) {
            this.incoming = incoming;
            this.connection = connection;
        }
    }
}
