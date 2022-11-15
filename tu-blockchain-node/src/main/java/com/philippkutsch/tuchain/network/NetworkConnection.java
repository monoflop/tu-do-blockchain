package com.philippkutsch.tuchain.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class NetworkConnection implements Runnable {
    private static final String ESCAPE_DELIMITER = "\n";
    private static final Logger logger
            = LoggerFactory.getLogger(NetworkConnection.class);

    private final Socket socket;
    private final Listener listener;
    private PrintWriter printWriter;
    private InputStream inputStream;
    private boolean hasError;

    public NetworkConnection(@Nonnull Socket socket,
                             @Nonnull Listener listener) {
        this.socket = socket;
        this.listener = listener;
        this.hasError = false;
    }

    @Override
    public void run() {
        try {
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            inputStream = socket.getInputStream();

            Scanner scanner = new Scanner(inputStream).useDelimiter(ESCAPE_DELIMITER);
            while (scanner.hasNext()) {
                String result = scanner.next();
                listener.onMessage(result);
            }
            listener.onDisconnected();
        }
        catch (IOException e) {
            logger.error("Connection problem", e);
            hasError = true;
            listener.onDisconnected();
        }
    }

    public void send(@Nonnull String message) throws IOException {
        if(hasError || printWriter == null) {
            throw new IOException("Disconnected or in error state");
        }
        printWriter.printf(message + ESCAPE_DELIMITER);
    }

    public void shutdown() throws IOException {
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

    public interface Listener {
        void onMessage(@Nonnull String message);
        void onDisconnected();
    }
}
