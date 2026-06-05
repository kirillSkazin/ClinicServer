package org.example.clinic.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.clinic.server.exception.AuthException;
import org.example.clinic.server.exception.ForbiddenException;
import org.example.clinic.server.exception.ServiceException;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.network.protocol.Request;
import org.example.clinic.server.network.protocol.Response;
import org.example.clinic.server.security.AuthPrincipal;
import org.example.clinic.server.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class RequestDispatcher {

    private static final Logger log = LoggerFactory.getLogger(RequestDispatcher.class);

    private final Map<String, RouteRegistration> routes = new HashMap<>();
    private final JwtService jwtService;

    public RequestDispatcher(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public RequestDispatcher register(String command, boolean requiresAuth,
                                      Role[] allowedRoles, Handler handler) {
        if (routes.put(command, new RouteRegistration(command, requiresAuth,
                allowedRoles == null ? new Role[0] : allowedRoles, handler)) != null) {
            log.warn("Duplicate registration for command '{}'", command);
        }
        return this;
    }

    public Response dispatch(Request request) {
        String reqId = request == null ? null : request.getId();
        if (request == null || request.getCommand() == null) {
            return Response.error(reqId, "BAD_REQUEST", "Команда не указана");
        }
        RouteRegistration route = routes.get(request.getCommand());
        if (route == null) {
            return Response.error(reqId, "UNKNOWN_COMMAND",
                    "Команда не поддерживается: " + request.getCommand());
        }
        try {
            AuthPrincipal principal = null;
            if (route.requiresAuth()) {
                if (request.getToken() == null || request.getToken().isBlank()) {
                    throw new AuthException("Требуется авторизация");
                }
                principal = jwtService.parse(request.getToken());
                if (route.allowedRoles().length > 0
                        && !principal.hasAnyRole(route.allowedRoles())) {
                    throw new ForbiddenException("Недостаточно прав");
                }
            }
            JsonNode payload = request.getPayload();
            Object result = route.handler().handle(payload, principal);
            return Response.ok(reqId, result);
        } catch (ServiceException ex) {
            return Response.error(reqId, ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error for command '{}': {}",
                    request.getCommand(), ex.getMessage(), ex);
            return Response.error(reqId, "INTERNAL_ERROR",
                    "Внутренняя ошибка сервера");
        }
    }

    public Map<String, RouteRegistration> routes() {
        return Collections.unmodifiableMap(routes);
    }
}
