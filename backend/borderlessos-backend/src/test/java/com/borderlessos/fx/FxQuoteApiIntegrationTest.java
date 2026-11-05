package com.borderlessos.fx;

import com.borderlessos.TestcontainersConfiguration;
import com.borderlessos.fx.provider.FxRate;
import com.borderlessos.fx.repository.FxQuoteRepository;
import com.borderlessos.fx.service.FxRateProviderService;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.service.UserService;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FxQuoteApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private FxQuoteRepository quoteRepository;

    @MockitoBean
    private FxRateProviderService rateProviderService;

    private User createUser(String role) {
        return userService.createUser(
                "fx-" + UUID.randomUUID() + "@example.test",
                "TestPassword123!",
                "FX Test User",
                role
        );
    }

    private String requestBody(
            String source,
            String destination,
            String amount
    ) {
        return """
                {
                    "sourceCurrency": "%s",
                    "destinationCurrency": "%s",
                    "sourceAmount": %s
                }
                """.formatted(source, destination, amount);
    }

    private FxRate mockRate(
            String source,
            String destination,
            String rate
    ) {
        return new FxRate(
                source,
                destination,
                new BigDecimal(rate),
                LocalDate.now(),
                "mock"
        );
    }

    @Test
    void customerShouldCreateFxQuoteSuccessfully() throws Exception {

        User customer = createUser("CUSTOMER");

        when(rateProviderService.fetchRate("INR", "JPY"))
                .thenReturn(mockRate("INR", "JPY", "1.72"));

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "1000")))

                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.quoteId").exists())
                .andExpect(jsonPath("$.sourceCurrency").value("INR"))
                .andExpect(jsonPath("$.destinationCurrency").value("JPY"))
                .andExpect(jsonPath("$.sourceAmount").value(1000))
                .andExpect(jsonPath("$.destinationAmount").value(1720))
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.rateDate").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.consumed").value(false));

        User persistedCustomer = customer;

        assert quoteRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(
                        persistedCustomer.getId()
                )
                .size() == 1;
    }

    @Test
    void unauthenticatedUserShouldNotCreateQuote() throws Exception {

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "1000")))

                .andExpect(status().isUnauthorized());
    }

    @Test
    void operationsUserShouldNotCreateQuote() throws Exception {

        User operations = createUser("OPERATIONS");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(operations.getEmail()).roles("OPERATIONS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "1000")))

                .andExpect(status().isForbidden());
    }

    @Test
    void complianceUserShouldNotCreateQuote() throws Exception {

        User compliance = createUser("COMPLIANCE");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(compliance.getEmail()).roles("COMPLIANCE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "1000")))

                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldNotCreateCustomerQuote() throws Exception {

        User admin = createUser("ADMIN");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(admin.getEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "1000")))

                .andExpect(status().isForbidden());
    }

    @Test
    void customerShouldNotCreateQuoteWithoutCsrfToken() throws Exception {

        User customer = createUser("CUSTOMER");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "1000")))

                .andExpect(status().isForbidden());
    }

    @Test
    void customerShouldNotCreateQuoteWithInvalidCurrencyFormat()
            throws Exception {

        User customer = createUser("CUSTOMER");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("IN", "JPY", "1000")))

                .andExpect(status().isBadRequest());
    }

    @Test
    void customerShouldNotCreateQuoteWithZeroAmount() throws Exception {

        User customer = createUser("CUSTOMER");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "0")))

                .andExpect(status().isBadRequest());
    }

    @Test
    void customerShouldNotCreateQuoteWithNegativeAmount() throws Exception {

        User customer = createUser("CUSTOMER");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INR", "JPY", "-100")))

                .andExpect(status().isBadRequest());
    }

    @Test
    void customerShouldNotCreateQuoteWithUnsupportedCurrencyPair()
            throws Exception {

        User customer = createUser("CUSTOMER");

        mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("USD", "EUR", "100")))

                .andExpect(status().isBadRequest());
    }

    private String createQuote(
            User customer,
            String source,
            String destination,
            String amount
    ) throws Exception {

        when(rateProviderService.fetchRate(source, destination))
                .thenReturn(mockRate(source, destination, "1.5"));

        MvcResult result = mockMvc.perform(post("/api/v1/fx/quotes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(source, destination, amount)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.quoteId"
        );
    }

    @Test
    void customerShouldReadOwnQuote() throws Exception {
        User customer = createUser("CUSTOMER");
        String quoteId = createQuote(customer, "INR", "JPY", "1000");

        mockMvc.perform(get("/api/v1/fx/quotes/{id}", quoteId)
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").value(quoteId))
                .andExpect(jsonPath("$.sourceCurrency").value("INR"))
                .andExpect(jsonPath("$.destinationCurrency").value("JPY"))
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.consumed").value(false));
    }

    @Test
    void customerShouldNotReadAnotherCustomersQuote() throws Exception {
        User owner = createUser("CUSTOMER");
        User otherCustomer = createUser("CUSTOMER");
        String quoteId = createQuote(owner, "INR", "JPY", "1000");

        mockMvc.perform(get("/api/v1/fx/quotes/{id}", quoteId)
                        .with(user(otherCustomer.getEmail())
                                .roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void nonCustomerShouldNotReadQuote() throws Exception {
        User customer = createUser("CUSTOMER");
        User operations = createUser("OPERATIONS");
        String quoteId = createQuote(customer, "INR", "JPY", "1000");

        mockMvc.perform(get("/api/v1/fx/quotes/{id}", quoteId)
                        .with(user(operations.getEmail()).roles("OPERATIONS")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserShouldNotReadQuote() throws Exception {
        User customer = createUser("CUSTOMER");
        String quoteId = createQuote(customer, "INR", "JPY", "1000");

        mockMvc.perform(get("/api/v1/fx/quotes/{id}", quoteId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getQuoteShouldRejectInvalidId() throws Exception {
        User customer = createUser("CUSTOMER");

        mockMvc.perform(get("/api/v1/fx/quotes/not-a-uuid")
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void getQuoteShouldReturnNotFoundForUnknownId() throws Exception {
        User customer = createUser("CUSTOMER");

        mockMvc.perform(get("/api/v1/fx/quotes/{id}", UUID.randomUUID())
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
