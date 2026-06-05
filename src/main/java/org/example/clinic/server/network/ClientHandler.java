package org.example.clinic.server.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.clinic.server.network.protocol.JsonMapper;
import org.example.clinic.server.network.protocol.Request;
import org.example.clinic.server.network.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;


public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private static final ObjectMapper JSON = JsonMapper.get();

    private final Socket socket;
    private final RequestDispatcher dispatcher;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        log.debug("Client connected: {}", remote);
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Response response;
                try {
                    Request request = JSON.readValue(line, Request.class);
                    response = dispatcher.dispatch(request);
                } catch (Exception parseEx) {
                    log.warn("Failed to parse request from {}: {}", remote, parseEx.getMessage());
                    response = Response.error(null, "BAD_REQUEST",
                            "Невалидный JSON: " + parseEx.getMessage());
                }
                String json = JSON.writeValueAsString(response);
                out.write(json);
                out.write('\n');
                out.flush();
            }
        } catch (SocketTimeoutException ste) {
            log.debug("Client {} timed out", remote);
        } catch (SocketException se) {
            log.debug("Client {} disconnected: {}", remote, se.getMessage());
        } catch (IOException ioe) {
            log.warn("I/O error with client {}: {}", remote, ioe.getMessage());
        } finally {
            log.debug("Client disconnected: {}", remote);
        }
    }
}
