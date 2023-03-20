package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.*;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

@Ignore
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
}
