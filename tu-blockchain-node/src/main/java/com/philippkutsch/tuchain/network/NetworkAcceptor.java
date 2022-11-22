package com.philippkutsch.tuchain.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

public class NetworkAcceptor implements Callable<Socket> {
    private final ServerSocket serverSocket;

    public NetworkAcceptor(
            int port)
            throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public Socket call() throws IOException {
        return serverSocket.accept();
    }

    public void shutdown() throws IOException {
        serverSocket.close();
    }
}
