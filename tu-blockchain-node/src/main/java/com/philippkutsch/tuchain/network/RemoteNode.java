package com.philippkutsch.tuchain.network;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.network.protocol.Message;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class RemoteNode implements Runnable, NetworkConnection.Listener {
    private static final Logger logger
            = LoggerFactory.getLogger(RemoteNode.class);

    private final NetworkConnection connection;
    private MessageListener messageListener;
    private DisconnectListener disconnectListener;
    private final Gson gson;

    private String name;
    private int listenPort;
    private boolean authenticated;

    public static Optional<Socket> tryConnect(@Nonnull String ip, int port) {
        try {
            return Optional.of(new Socket(ip, port));
        }
        catch (IOException e) {
            logger.warn("tryConnect failed", e);
        }
        return Optional.empty();
    }

    public RemoteNode(@Nonnull Socket socket,
                      @Nonnull MessageListener messageListener,
                      @Nonnull DisconnectListener disconnectListener) {
        this.connection = new NetworkConnection(socket, this);
        this.messageListener = messageListener;
        this.disconnectListener = disconnectListener;
        this.gson = new Gson();
        this.authenticated = false;
    }

    public void send(@Nonnull Message message) {
        logger.debug("Send to " + name + " raw message: " + ChainUtils.encodeToString(message));
        String rawMessage = gson.toJson(message);
        try {
            connection.send(rawMessage);
        }
        catch (IOException e) {
            logger.error("Failed to send message to node", e);
            onDisconnected();
        }
    }

    public void shutdown() throws IOException {
        connection.shutdown();
    }

    @Override
    public void onMessage(@Nonnull String message) {
        logger.debug("Received raw message: " + message);
        try {
            Message parsedMessage = gson.fromJson(message, Message.class);
            messageListener.onInternalMessage(this, parsedMessage);
        }
        catch (JsonParseException e) {
            logger.error("Failed to parse message", e);
        }
    }

    @Override
    public void onDisconnected() {
        disconnectListener.onNodeDisconnected(this);
    }

    @Override
    public void run() {
        connection.run();
    }

    public void updateListener(
            @Nonnull MessageListener messageListener,
            @Nonnull DisconnectListener disconnectListener) {
        this.messageListener = messageListener;
        this.disconnectListener = disconnectListener;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public NetworkConnection getConnection() {
        return connection;
    }

    public interface MessageListener {
        void onInternalMessage(@NotNull RemoteNode remoteNode, @NotNull Message message);
    }

    public interface DisconnectListener {
        void onNodeDisconnected(@Nonnull RemoteNode remoteNode);
    }
}
