package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.philippkutsch.tuchain.network.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle all remote nodes
 *
 * - Try to create a remote node from host and port
 * - Protocol: Client connection sends handshake first
 */
public class ConnectionManager implements NetworkConnection.Listener, NetworkServer.Listener {
    private static final Logger logger
            = LoggerFactory.getLogger(ConnectionManager.class);
    private final ListeningExecutorService service;
    private final NetworkServer server;
    private final Map<String, NetworkConnection> connectionMap;
    private final Gson gson;
    private final Listener listener;

    public ConnectionManager(@Nonnull ListeningExecutorService service,
                             int listeningPort,
                             @Nonnull Listener listener)
            throws IOException {
        this.service = service;
        this.listener = listener;
        this.server = new NetworkServer(service, listeningPort, this);
        this.connectionMap = new ConcurrentHashMap<>();
        this.gson = new Gson();
    }

    public void connect(@Nonnull String host, int port) {
        //Check if connection is running
        if(connectionMap.containsKey(host + ":" + port)) {
            logger.debug("Already connecting / connected to " + host + ":" + port);
            return;
        }

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 30000);
            onSocketConnected(false, socket);
        }
        catch (IOException e) {
            logger.error("Connection failed", e);
        }
    }

    public void disconnect(@Nonnull NetworkConnection connection) {
        onDisconnected(connection);
    }

    public void send(@Nonnull NetworkConnection connection,
                     @Nonnull Message message) {
        try {
            String rawMessage = gson.toJson(message);
            connection.send(rawMessage);
        }
        catch (IOException e) {
            logger.error("Failed to send message", e);
        }
    }

    @Override
    public void onMessage(@Nonnull NetworkConnection connection,
                          @Nonnull String message) {
        //Incoming message from network connection
        //Parse message
        try {
            Message parsedMessage = gson.fromJson(message, Message.class);
            listener.onMessage(connection, parsedMessage);
        }
        catch (JsonParseException e) {
            logger.warn("Failed to parse message", e);
        }
    }

    @Override
    public void onDisconnected(@Nonnull NetworkConnection connection) {
        logger.debug("onDisconnected");
        try {
            NetworkConnection storedConnection = connectionMap.get(connection.getKey());
            if(storedConnection != null) {
                storedConnection.shutdown();
                connectionMap.remove(connection.getKey());
                listener.onDisconnected(connection);
            }
        }
        catch (IOException e) {
            logger.error("Failed to close connection", e);
        }
    }

    @Override
    public void onSocketConnected(boolean incoming, @Nonnull Socket socket) {
        try {
            //Create network connection and
            NetworkConnection networkConnection = new NetworkConnection(service, socket, this);
            connectionMap.put(networkConnection.getKey(), networkConnection);
            listener.onConnected(incoming, networkConnection);
        }
        catch (IOException e) {
            logger.error("Failed to setup connection", e);
        }
    }

    public void shutdown() throws IOException {
        for (Map.Entry<String, NetworkConnection> connection : connectionMap.entrySet()) {
            connection.getValue().shutdown();
        }

        server.shutdown();
    }

    public interface Listener {
        void onConnected(boolean incoming, @Nonnull NetworkConnection connection);
        void onDisconnected(@Nonnull NetworkConnection connection);
        void onMessage(@Nonnull NetworkConnection connection, @Nonnull Message message);
    }
}
