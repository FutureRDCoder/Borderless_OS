package com.borderlessos.admin.dto.response;

import com.borderlessos.identity.entity.Role;
import com.borderlessos.identity.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        boolean enabled,
        Set<String> roles,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEnabled(),
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
