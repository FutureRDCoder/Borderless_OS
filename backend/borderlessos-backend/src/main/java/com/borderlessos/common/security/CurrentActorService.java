package com.borderlessos.common.security;

import com.borderlessos.identity.entity.Role;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.UUID;

@Service
public class CurrentActorService {

    public static final CurrentActor SYSTEM = new CurrentActor(
            null,
            "system@borderlessos.local",
            "SYSTEM"
    );

    private final UserRepository userRepository;

    public CurrentActorService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentActor requireActor() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Authentication is required.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found."
                ));

        String role = user.getRoles()
                .stream()
                .map(Role::getName)
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse("CUSTOMER");

        return new CurrentActor(user.getId(), user.getEmail(), role);
    }

    public UUID requireCustomerId() {
        CurrentActor actor = requireActor();

        if (!actor.hasRole(AppRoles.CUSTOMER)) {
            throw new IllegalStateException("Customer access is required.");
        }

        return actor.id();
    }
}
