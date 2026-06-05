package org.example.clinic.server.network;

import org.example.clinic.server.model.Role;


public record RouteRegistration(String command,
                                boolean requiresAuth,
                                Role[] allowedRoles,
                                Handler handler) {
}
