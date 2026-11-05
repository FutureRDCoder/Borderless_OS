package com.borderlessos.identity.security;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RbacTestController.class)
@Testcontainers
class RbacSecurityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("borderlessos_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
                postgres::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    // 1. Unauthenticated request

    @Test
    void unauthenticatedUserShouldReceive401() throws Exception {

        mockMvc.perform(get("/test/rbac/customer"))
                .andExpect(status().isUnauthorized());
    }

    // 2. CUSTOMER authorization

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerShouldAccessCustomerEndpoint() throws Exception {

        mockMvc.perform(get("/test/rbac/customer"))
                .andExpect(status().isOk())
                .andExpect(content().string("Customer access granted"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerShouldNotAccessOperationsEndpoint() throws Exception {

        mockMvc.perform(get("/test/rbac/operations"))
                .andExpect(status().isForbidden());
    }

    // 3. OPERATIONS authorization

    @Test
    @WithMockUser(roles = "OPERATIONS")
    void operationsShouldAccessOperationsEndpoint() throws Exception {

        mockMvc.perform(get("/test/rbac/operations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATIONS")
    void operationsShouldNotAccessComplianceEndpoint() throws Exception {

        mockMvc.perform(get("/test/rbac/compliance"))
                .andExpect(status().isForbidden());
    }

    // 4. COMPLIANCE authorization

    @Test
    @WithMockUser(roles = "COMPLIANCE")
    void complianceShouldAccessComplianceEndpoint() throws Exception {

        mockMvc.perform(get("/test/rbac/compliance"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE")
    void complianceShouldNotAccessOperationsEndpoint() throws Exception {

        mockMvc.perform(get("/test/rbac/operations"))
                .andExpect(status().isForbidden());
    }

    // 5. ADMIN authorization

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessAdminEndpoint() throws Exception {

        mockMvc.perform(get("/test/rbac/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldNotAutomaticallyAccessComplianceEndpoint()
            throws Exception {

        mockMvc.perform(get("/test/rbac/compliance"))
                .andExpect(status().isForbidden());
    }
}