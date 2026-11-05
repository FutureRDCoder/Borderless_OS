package com.borderlessos.beneficiary;

import com.borderlessos.TestcontainersConfiguration;
import com.borderlessos.identity.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import com.borderlessos.beneficiary.service.BeneficiaryService;
import com.borderlessos.identity.entity.User;

import org.hamcrest.Matchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BeneficiaryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private BeneficiaryService beneficiaryService;

    @Test
    void customerShouldCreateBeneficiarySuccessfully() throws Exception {

        // Arrange: Create a real synthetic CUSTOMER in PostgreSQL.
        String email = "customer-" + UUID.randomUUID() + "@example.test";

        userService.createUser(
                email,
                "TestPassword123!",
                "Test Customer",
                "CUSTOMER"
        );

        String requestBody = """
                {
                    "recipientName": "Alex Morgan",
                    "destinationCountry": "US",
                    "destinationCurrency": "USD",
                    "accountReference": "DEMO-ACCOUNT-001"
                }
                """;

        // Act: Authenticate as that customer and create a beneficiary.
        mockMvc.perform(post("/api/v1/beneficiaries")
                        .with(user(email).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                // Assert: Verify the API response.
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.recipientName").value("Alex Morgan"))
                .andExpect(jsonPath("$.destinationCountry").value("US"))
                .andExpect(jsonPath("$.destinationCurrency").value("USD"))
                .andExpect(jsonPath("$.accountReference").value("DEMO-ACCOUNT-001"))
                .andExpect(jsonPath("$.active").isBoolean())
                .andExpect(jsonPath("$.active").value(Matchers.equalTo(true)))
                .andExpect(header().exists("Location"));
    }

    @Test
    void customerShouldNotAccessAnotherCustomersBeneficiary() throws Exception {

        // Arrange: Create two independent customers.
        String customerAEmail =
                "customer-a-" + UUID.randomUUID() + "@example.test";

        String customerBEmail =
                "customer-b-" + UUID.randomUUID() + "@example.test";

        User customerA = userService.createUser(
                customerAEmail,
                "TestPassword123!",
                "Customer A",
                "CUSTOMER"
        );

        User customerB = userService.createUser(
                customerBEmail,
                "TestPassword123!",
                "Customer B",
                "CUSTOMER"
        );

        // Create a beneficiary owned by Customer B.
        var beneficiary = beneficiaryService.createBeneficiary(
                customerB.getId(),
                "Sarah Williams",
                "US",
                "USD",
                "DEMO-ACCOUNT-002"
        );

        // Act: Customer A attempts to retrieve Customer B's beneficiary.
        mockMvc.perform(get("/api/v1/beneficiaries/{id}", beneficiary.getId())
                        .with(user(customerAEmail).roles("CUSTOMER")))
                // Assert: The resource must appear not to exist.
                .andExpect(status().isNotFound());
    }

    @Test
    void customerShouldNotDeactivateAnotherCustomersBeneficiary() throws Exception {

        // Arrange: Create two customers.
        String customerAEmail =
                "customer-a-" + UUID.randomUUID() + "@example.test";

        String customerBEmail =
                "customer-b-" + UUID.randomUUID() + "@example.test";

        User customerA = userService.createUser(
                customerAEmail,
                "TestPassword123!",
                "Customer A",
                "CUSTOMER"
        );

        User customerB = userService.createUser(
                customerBEmail,
                "TestPassword123!",
                "Customer B",
                "CUSTOMER"
        );

        // Create a beneficiary belonging to Customer B.
        var beneficiary = beneficiaryService.createBeneficiary(
                customerB.getId(),
                "Sarah Williams",
                "US",
                "USD",
                "DEMO-ACCOUNT-003"
        );

        // Act: Customer A attempts to deactivate Customer B's beneficiary.
        mockMvc.perform(delete("/api/v1/beneficiaries/{id}", beneficiary.getId())
                        .with(user(customerAEmail).roles("CUSTOMER"))
                        .with(csrf()))
                // Assert: Access must be denied without revealing ownership.
                .andExpect(status().isNotFound());

        // Assert: The beneficiary must remain active.
        var unchangedBeneficiary = beneficiaryService.getMyBeneficiary(
                customerB.getId(),
                beneficiary.getId()
        );

        org.junit.jupiter.api.Assertions.assertTrue(
                unchangedBeneficiary.isActive()
        );
    }

    @Test
    void unauthenticatedUserShouldNotAccessBeneficiaries() throws Exception {

        mockMvc.perform(get("/api/v1/beneficiaries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operationsUserShouldNotAccessCustomerBeneficiaries() throws Exception {

        String operationsEmail =
                "operations-" + UUID.randomUUID() + "@example.test";

        userService.createUser(
                operationsEmail,
                "TestPassword123!",
                "Operations User",
                "OPERATIONS"
        );

        mockMvc.perform(get("/api/v1/beneficiaries")
                        .with(user(operationsEmail).roles("OPERATIONS")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerShouldNotCreateBeneficiaryWithoutRecipientName() throws Exception {

        String email = "validation.customer@borderlessos.test";

        userService.createUser(
                email,
                "TestPassword@123",
                "Validation Customer",
                "CUSTOMER"
        );

        String requestBody = """
            {
                "destinationCountry": "JP",
                "destinationCurrency": "JPY",
                "accountReference": "JP123456789"
            }
            """;

        mockMvc.perform(post("/api/v1/beneficiaries")
                        .with(user(email).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.recipientName")
                        .value("Recipient name is required."));
    }

    @Test
    void customerShouldNotCreateBeneficiaryWithInvalidCurrencyCode() throws Exception {

        String email = "currency.validation@borderlessos.test";

        userService.createUser(
                email,
                "TestPassword@123",
                "Currency Validation Customer",
                "CUSTOMER"
        );

        String requestBody = """
            {
                "recipientName": "Yuki Tanaka",
                "destinationCountry": "JP",
                "destinationCurrency": "JP",
                "accountReference": "JP123456789"
            }
            """;

        mockMvc.perform(post("/api/v1/beneficiaries")
                        .with(user(email).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.destinationCurrency")
                        .value("Destination currency must be a three-letter currency code."));
    }

    @Test
    void customerShouldNotCreateBeneficiaryWithInvalidCountryCode() throws Exception {

        String email = "country.validation@borderlessos.test";

        userService.createUser(
                email,
                "TestPassword@123",
                "Country Validation Customer",
                "CUSTOMER"
        );

        String requestBody = """
            {
                "recipientName": "Yuki Tanaka",
                "destinationCountry": "JPN",
                "destinationCurrency": "JPY",
                "accountReference": "JP123456789"
            }
            """;

        mockMvc.perform(post("/api/v1/beneficiaries")
                        .with(user(email).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.destinationCountry")
                        .value("Destination country must be a two-letter country code."));
    }
}