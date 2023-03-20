package com.philippkutsch.tuchain.network;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Low level network connection
 */
public class NetworkConnection implements Callable<Void>, FutureCallback<Void> {
    private static final String ESCAPE_DELIMITER = "\n";
    private static final Logger logger
            = LoggerFactory.getLogger(NetworkConnection.class);

    private final Socket socket;
    private final Listener listener;
    private final ListenableFuture<Void> socketReadingFuture;
    private final PrintWriter printWriter;
    private final InputStream inputStream;
    private final Scanner scanner;

    private boolean isShuttingDown;

    public NetworkConnection(
            @Nonnull ListeningExecutorService service,
            @Nonnull Socket socket,
            @Nonnull Listener listener)
            throws IOException {
        this.socket = socket;
        this.listener = listener;

        //Setup socket
        this.printWriter = new PrintWriter(socket.getOutputStream(), true);
        this.inputStream = socket.getInputStream();
        this.scanner = new Scanner(inputStream).useDelimiter(ESCAPE_DELIMITER);

        //Start listening for incoming messages
        this.socketReadingFuture = service.submit(this);
        Futures.addCallback(this.socketReadingFuture, this, service);

        isShuttingDown = false;
    }

    /**
     * Setup socket connection and listen for incoming messages.
     * Executed on a seperate thread, because scanner is blocking
     */
    @Override
    public Void call() throws Exception {
        while (scanner.hasNext()) {
            String result = scanner.next();
            listener.onMessage(this, result);
        }
        return null;
    }

    /**
     * Reading future ended
     */
    @Override
    public void onSuccess(Void unused) {
        listener.onDisconnected(this);
    }

    /**
     * Reading future exception
     */
    @Override
    public void onFailure(@Nonnull Throwable throwable) {
        //Only redirect error if connection is not already shutting down
        if(!isShuttingDown) {
            logger.error("Reading error", throwable);
            listener.onDisconnected(this);
        }

    }

    public void send(@Nonnull String message) throws IOException {
        printWriter.printf(message + ESCAPE_DELIMITER);
    }

    public void shutdown() throws IOException {
        isShuttingDown = true;
        socketReadingFuture.cancel(true);
        inputStream.close();
        printWriter.close();
        socket.close();
    }

    public int getPort() {
        return socket.getPort();
    }

    public String getAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    @Nonnull
    public String getKey() {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public interface Listener {
        void onMessage(@Nonnull NetworkConnection connection, @Nonnull String message);
        void onDisconnected(@Nonnull NetworkConnection connection);
    }
}
