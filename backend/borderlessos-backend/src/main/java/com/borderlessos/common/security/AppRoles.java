package com.borderlessos.common.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class AppRoles {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String OPERATIONS = "OPERATIONS";
    public static final String COMPLIANCE = "COMPLIANCE";
    public static final String ADMIN = "ADMIN";
    public static final String SYSTEM = "SYSTEM";

    private static final List<String> DEFAULT_ROLE_PRIORITY =
            List.of(CUSTOMER, OPERATIONS, COMPLIANCE, ADMIN, SYSTEM);

    public String customer() {
        return CUSTOMER;
    }

    public String operations() {
        return OPERATIONS;
    }

    public String compliance() {
        return COMPLIANCE;
    }

    public String admin() {
        return ADMIN;
    }

    public String defaultRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return CUSTOMER;
        }

        for (String candidate : DEFAULT_ROLE_PRIORITY) {
            if (roles.contains(candidate)) {
                return candidate;
            }
        }

        return roles.stream().sorted().findFirst().orElse(CUSTOMER);
    }
}
