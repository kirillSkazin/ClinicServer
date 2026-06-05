package org.example.clinic.server.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.clinic.server.exception.AuthException;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.network.protocol.JsonMapper;
import org.example.clinic.server.network.protocol.Request;
import org.example.clinic.server.network.protocol.Response;
import org.example.clinic.server.security.AuthPrincipal;
import org.example.clinic.server.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestDispatcherTest {

    private static final String SECRET =
            "test-secret-test-secret-test-secret-test-secret-test-secret";

    private final JwtService jwt = new JwtService(SECRET, "test", 60);
    private final ObjectMapper mapper = JsonMapper.get();

    @Test
    void unknownCommandReturnsError() {
        RequestDispatcher d = new RequestDispatcher(jwt);
        Request req = new Request();
        req.setId("1");
        req.setCommand("does.not.exist");

        Response r = d.dispatch(req);
        assertFalse(r.isSuccess());
        assertEquals("UNKNOWN_COMMAND", r.getErrorCode());
    }

    @Test
    void anonymousCommandWorksWithoutToken() {
        RequestDispatcher d = new RequestDispatcher(jwt);
        d.register("ping", false, null, (payload, principal) -> "pong");
        Request req = new Request();
        req.setId("1");
        req.setCommand("ping");
        Response r = d.dispatch(req);
        assertTrue(r.isSuccess());
        assertEquals("pong", r.getData());
    }

    @Test
    void protectedCommandFailsWithoutToken() {
        RequestDispatcher d = new RequestDispatcher(jwt);
        d.register("secret", true, null, (payload, principal) -> "ok");
        Request req = new Request();
        req.setId("1");
        req.setCommand("secret");
        Response r = d.dispatch(req);
        assertFalse(r.isSuccess());
        assertEquals("UNAUTHORIZED", r.getErrorCode());
    }

    @Test
    void protectedCommandSucceedsWithValidToken() {
        RequestDispatcher d = new RequestDispatcher(jwt);
        d.register("me", true, null, (payload, principal) -> principal.username());

        String token = jwt.issueToken(1L, "alice", Role.ADMIN);
        Request req = new Request();
        req.setId("1");
        req.setCommand("me");
        req.setToken(token);

        Response r = d.dispatch(req);
        assertTrue(r.isSuccess());
        assertEquals("alice", r.getData());
    }

    @Test
    void roleRestrictionEnforced() {
        RequestDispatcher d = new RequestDispatcher(jwt);
        d.register("admin-only", true, new Role[]{Role.ADMIN},
                (payload, principal) -> "ok");
        String patientToken = jwt.issueToken(1L, "p", Role.PATIENT);

        Request req = new Request();
        req.setId("1");
        req.setCommand("admin-only");
        req.setToken(patientToken);

        Response r = d.dispatch(req);
        assertFalse(r.isSuccess());
        assertEquals("FORBIDDEN", r.getErrorCode());
    }

    @Test
    void serviceExceptionsArePropagatedAsErrorCodes() {
        RequestDispatcher d = new RequestDispatcher(jwt);
        d.register("validate", false, null, (payload, principal) -> {
            throw new ValidationException("bad input");
        });
        Request req = new Request();
        req.setId("1");
        req.setCommand("validate");
        Response r = d.dispatch(req);
        assertFalse(r.isSuccess());
        assertEquals("VALIDATION_ERROR", r.getErrorCode());
        assertEquals("bad input", r.getError());
    }

    @Test
    void uncheckedExceptionFromHandlerIsAuthError() {
        RequestDispatcher d = new RequestDispatcher(jwt);
        d.register("auth-fail", false, null, (payload, principal) -> {
            throw new AuthException("nope");
        });
        Request req = new Request();
        req.setId("1");
        req.setCommand("auth-fail");
        Response r = d.dispatch(req);
        assertFalse(r.isSuccess());
        assertEquals("UNAUTHORIZED", r.getErrorCode());
    }
}
