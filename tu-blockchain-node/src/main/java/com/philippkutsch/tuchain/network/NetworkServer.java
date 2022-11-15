package com.philippkutsch.tuchain.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkServer {
    private static final Logger logger
            = LoggerFactory.getLogger(NetworkServer.class);

    private final int port;
    private final Listener listener;
    private volatile boolean listening = true;
    private ServerSocket serverSocket;

    public NetworkServer(int port,
                         @Nonnull Listener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void startListen() throws IOException {
        this.serverSocket = new ServerSocket(port);
        while (listening) {
            listener.onSocketConnected(serverSocket.accept());
        }
    }

    public void stopListen() throws IOException {
        listening = false;
        serverSocket.close();
    }

    public interface Listener {
        void onSocketConnected(@Nonnull Socket socket);
    }
}
