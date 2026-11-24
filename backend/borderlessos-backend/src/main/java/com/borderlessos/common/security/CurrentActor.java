package com.borderlessos.common.security;

import java.util.Set;
import java.util.UUID;

public record CurrentActor(
        UUID id,
        String email,
        String role,
        Set<String> roles
) {

    public CurrentActor(UUID id, String email, String role) {
        this(id, email, role, Set.of(role));
    }

    public boolean hasRole(String expectedRole) {
        return expectedRole != null
                && roles != null
                && roles.contains(expectedRole);
    }
}
