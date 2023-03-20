package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Socket;

/**
 * NetworkServer
 *
 * Listens for new sockets
 */
public class NetworkServer implements FutureCallback<Socket> {
    private static final Logger logger
            = LoggerFactory.getLogger(NetworkServer.class);

    private final ListeningExecutorService service;
    private final NetworkAcceptor networkAcceptor;
    private final Listener listener;

    private ListenableFuture<Socket> serverListenFuture;

    public NetworkServer(
            @Nonnull ListeningExecutorService service,
            int listeningPort,
            @Nonnull Listener listener)
            throws IOException {
        this.service = service;
        this.networkAcceptor = new NetworkAcceptor(listeningPort);
        this.listener = listener;
        listen();
    }

    private void listen() {
        this.serverListenFuture = service.submit(networkAcceptor);
        Futures.addCallback(this.serverListenFuture, this, service);
    }

    public void shutdown() throws IOException {
        if(serverListenFuture != null) {
            serverListenFuture.cancel(true);
        }
        this.networkAcceptor.shutdown();
    }

    //New client socket connected
    @Override
    public void onSuccess(@Nullable Socket socket) {
        if(socket == null) {
            return;
        }
        logger.debug("Incoming connection " + socket.getInetAddress() + ":" + socket.getPort());
        listener.onSocketConnected(true, socket);
        listen();
    }

    //Server socket listen exception
    @Override
    public void onFailure(@Nonnull Throwable throwable) {
        logger.error("Connection attempt failed", throwable);
    }

    public interface Listener {
        void onSocketConnected(boolean incoming, @Nonnull Socket socket);
    }
}
