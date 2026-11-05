package com.borderlessos.identity.service;

import com.borderlessos.identity.entity.Role;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.RoleRepository;
import com.borderlessos.identity.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(
            String email,
            String rawPassword,
            String displayName,
            String roleName
    ) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedRole = normalizeRole(roleName);

        validatePassword(rawPassword);
        validateDisplayName(displayName);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Email is already registered");
        }

        Role role = roleRepository.findByName(normalizedRole)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role does not exist")
                );

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(
                normalizedEmail,
                passwordHash,
                displayName.trim()
        );

        user.addRole(role);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User does not exist")
                );
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User does not exist")
                );
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 12) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters"
            );
        }
    }

    private void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }

        if (displayName.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Display name cannot exceed 120 characters"
            );
        }
    }
}