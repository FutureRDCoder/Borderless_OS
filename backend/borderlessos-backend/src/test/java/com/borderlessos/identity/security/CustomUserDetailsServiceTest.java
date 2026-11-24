package com.borderlessos.identity.security;

import com.borderlessos.identity.entity.Role;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = new User(
                "customer@borderlessos.local",
                "$2a$12$examplePasswordHash",
                "Demo Customer"
        );

        customer.addRole(
                new Role(
                        UUID.randomUUID(),
                        "CUSTOMER"
                )
        );
    }

    @Test
    void shouldLoadUserByEmail() {

        when(userRepository.findByEmail(
                "customer@borderlessos.local"
        )).thenReturn(Optional.of(customer));

        UserDetails result = userDetailsService.loadUserByUsername(
                "customer@borderlessos.local"
        );

        assertEquals(
                "customer@borderlessos.local",
                result.getUsername()
        );

        assertEquals(
                "$2a$12$examplePasswordHash",
                result.getPassword()
        );

        assertTrue(result.isEnabled());

        assertTrue(
                result.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_CUSTOMER"::equals)
        );

        verify(userRepository).findByEmail(
                "customer@borderlessos.local"
        );
    }

    @Test
    void shouldNormalizeEmailBeforeLookup() {

        when(userRepository.findByEmail(
                "customer@borderlessos.local"
        )).thenReturn(Optional.of(customer));

        UserDetails result = userDetailsService.loadUserByUsername(
                "  CUSTOMER@BORDERLESSOS.LOCAL  "
        );

        assertEquals(
                "customer@borderlessos.local",
                result.getUsername()
        );
    }

    @Test
    void shouldRejectUnknownUser() {

        when(userRepository.findByEmail(
                "unknown@borderlessos.local"
        )).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(
                        "unknown@borderlessos.local"
                )
        );
    }

    @Test
    void shouldLoadComplianceAuthority() {

        User compliance = new User(
                "compliance@borderlessos.local",
                "$2a$12$examplePasswordHash",
                "Demo Compliance"
        );
        compliance.addRole(
                new Role(
                        UUID.randomUUID(),
                        "COMPLIANCE"
                )
        );

        when(userRepository.findByEmail(
                "compliance@borderlessos.local"
        )).thenReturn(Optional.of(compliance));

        UserDetails result = userDetailsService.loadUserByUsername(
                "compliance@borderlessos.local"
        );

        assertTrue(result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_COMPLIANCE"::equals));
    }

    @Test
    void shouldLoadMultipleRoles() {

        customer.addRole(
                new Role(
                        UUID.randomUUID(),
                        "OPERATIONS"
                )
        );

        when(userRepository.findByEmail(
                "customer@borderlessos.local"
        )).thenReturn(Optional.of(customer));

        UserDetails result = userDetailsService.loadUserByUsername(
                "customer@borderlessos.local"
        );

        Set<String> authorities = result.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(authorities.contains("ROLE_CUSTOMER"));
        assertTrue(authorities.contains("ROLE_OPERATIONS"));
    }
}