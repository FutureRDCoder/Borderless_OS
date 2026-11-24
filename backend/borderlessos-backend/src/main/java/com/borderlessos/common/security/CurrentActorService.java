package com.borderlessos.common.security;

import com.borderlessos.identity.entity.Role;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurrentActorService {

    public static final CurrentActor SYSTEM = new CurrentActor(
            null,
            "system@borderlessos.local",
            AppRoles.SYSTEM
    );

    private final UserRepository userRepository;
    private final AppRoles appRoles;

    public CurrentActorService(UserRepository userRepository, AppRoles appRoles) {
        this.userRepository = userRepository;
        this.appRoles = appRoles;
    }

    public CurrentActor requireActor() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication is required.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user not found."
                ));

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return new CurrentActor(
                user.getId(),
                user.getEmail(),
                appRoles.defaultRole(roles),
                roles
        );
    }

    public CurrentActor requireRole(String expectedRole) {
        CurrentActor actor = requireActor();

        if (!actor.hasRole(expectedRole)) {
            throw new AccessDeniedException(
                    "Only the " + expectedRole + " role may perform this action."
            );
        }

        return new CurrentActor(
                actor.id(),
                actor.email(),
                expectedRole,
                actor.roles()
        );
    }

    public UUID requireCustomerId() {
        return requireRole(AppRoles.CUSTOMER).id();
    }
}
