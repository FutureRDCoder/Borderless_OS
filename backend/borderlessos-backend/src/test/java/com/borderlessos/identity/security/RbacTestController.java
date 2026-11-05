package com.borderlessos.identity.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/rbac")
public class RbacTestController {

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerEndpoint() {
        return "Customer access granted";
    }

    @GetMapping("/operations")
    @PreAuthorize("hasRole('OPERATIONS')")
    public String operationsEndpoint() {
        return "Operations access granted";
    }

    @GetMapping("/compliance")
    @PreAuthorize("hasRole('COMPLIANCE')")
    public String complianceEndpoint() {
        return "Compliance access granted";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "Admin access granted";
    }
}