package org.example.clinic.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class TcpServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    private final String host;
    private final int port;
    private final int backlog;
    private final int socketTimeoutMs;
    private final ExecutorService executor;
    private final RequestDispatcher dispatcher;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TcpServer(String host, int port, int backlog, int threadPoolSize,
                     int socketTimeoutMs, RequestDispatcher dispatcher) {
        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.socketTimeoutMs = socketTimeoutMs;
        this.dispatcher = dispatcher;
        this.executor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "client-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(host, port), backlog);
        log.info("TCP server listening on {}:{}", host, port);

        acceptThread = new Thread(this::acceptLoop, "tcp-accept");
        acceptThread.setDaemon(false);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                client.setSoTimeout(socketTimeoutMs);
                executor.submit(new ClientHandler(client, dispatcher));
            } catch (IOException ex) {
                if (running.get()) {
                    log.error("Accept failed", ex);
                }
            }
        }
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ex) {
            log.warn("Error closing server socket", ex);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (acceptThread != null) {
            try {
                acceptThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("TCP server stopped");
    }
}
