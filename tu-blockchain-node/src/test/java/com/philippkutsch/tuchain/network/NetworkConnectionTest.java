package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.*;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class NetworkConnectionTest {
    public ListeningExecutorService service;

    @Before
    public void setup() {
        this.service = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    }

    @After
    public void tearDown() {
        this.service.shutdown();
    }

    @Test
    public void connectionTest() throws IOException {
        /*NetworkServer networkServer = new NetworkServer(8000);
        ListenableFuture<Socket> socketListenableFuture = service.submit(networkServer);
        Futures.addCallback(socketListenableFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Socket socket) {

            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        }, service);



        NetworkConnection connection = new NetworkConnection(service, socket, new NetworkConnection.Listener() {
            @Override
            public void onMessage(@NotNull NetworkConnection connection, @NotNull String message) {

            }

            @Override
            public void onDisconnected(@NotNull NetworkConnection connection) {

            }
        });*/
    }
}
