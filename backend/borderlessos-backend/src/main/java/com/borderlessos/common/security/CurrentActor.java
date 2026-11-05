package com.borderlessos.common.security;

import java.util.UUID;

public record CurrentActor(
        UUID id,
        String email,
        String role
) {

    public boolean hasRole(String expectedRole) {
        return role != null && role.equals(expectedRole);
    }
}
