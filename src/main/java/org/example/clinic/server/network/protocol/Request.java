package org.example.clinic.server.network.protocol;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serial;
import java.io.Serializable;


public class Request implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String command;
    private String token;
    private JsonNode payload;

    public Request() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }
}
