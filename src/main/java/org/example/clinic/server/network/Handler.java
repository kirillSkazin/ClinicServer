package org.example.clinic.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.clinic.server.security.AuthPrincipal;


@FunctionalInterface
public interface Handler {

    

    Object handle(JsonNode payload, AuthPrincipal principal) throws Exception;
}
