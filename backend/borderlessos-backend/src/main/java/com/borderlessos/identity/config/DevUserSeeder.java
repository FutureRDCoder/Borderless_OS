package com.borderlessos.identity.config;

import com.borderlessos.identity.repository.UserRepository;
import com.borderlessos.identity.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevUserSeeder implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DevUserSeeder.class);

    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${borderlessos.demo-users.customer.email}")
    private String customerEmail;

    @Value("${borderlessos.demo-users.customer.display-name}")
    private String customerName;

    @Value("${borderlessos.demo-users.customer.password}")
    private String customerPassword;

    @Value("${borderlessos.demo-users.operations.email}")
    private String operationsEmail;

    @Value("${borderlessos.demo-users.operations.display-name}")
    private String operationsName;

    @Value("${borderlessos.demo-users.operations.password}")
    private String operationsPassword;

    @Value("${borderlessos.demo-users.compliance.email}")
    private String complianceEmail;

    @Value("${borderlessos.demo-users.compliance.display-name}")
    private String complianceName;

    @Value("${borderlessos.demo-users.compliance.password}")
    private String compliancePassword;

    @Value("${borderlessos.demo-users.admin.email}")
    private String adminEmail;

    @Value("${borderlessos.demo-users.admin.display-name}")
    private String adminName;

    @Value("${borderlessos.demo-users.admin.password}")
    private String adminPassword;

    public DevUserSeeder(
            UserService userService,
            UserRepository userRepository
    ) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {

        createIfAbsent(
                customerEmail, customerPassword, customerName, "CUSTOMER"
        );

        createIfAbsent(
                operationsEmail, operationsPassword, operationsName, "OPERATIONS"
        );

        createIfAbsent(
                complianceEmail, compliancePassword, complianceName, "COMPLIANCE"
        );

        createIfAbsent(
                adminEmail, adminPassword, adminName, "ADMIN"
        );

        log.info("Development demo-user provisioning completed.");
    }

    private void createIfAbsent(
            String email,
            String password,
            String displayName,
            String role
    ) {
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Demo account already exists for role: {}", role);
            return;
        }

        userService.createUser(
                normalizedEmail,
                password,
                displayName,
                role
        );

        log.info("Created development demo account for role: {}", role);
    }
}