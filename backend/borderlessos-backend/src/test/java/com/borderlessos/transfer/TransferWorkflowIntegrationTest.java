package com.borderlessos.transfer;

import com.borderlessos.TestcontainersConfiguration;
import com.borderlessos.fx.provider.FxRate;
import com.borderlessos.fx.service.FxRateProviderService;
import com.borderlessos.identity.service.UserService;
import com.borderlessos.transfer.entity.TransferAuditEvent;
import com.borderlessos.transfer.enums.TransferEventType;
import com.borderlessos.transfer.enums.TransferStatus;
import com.borderlessos.transfer.repository.TransferAuditEventRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TransferWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TransferAuditEventRepository auditRepository;

    @MockitoBean
    private FxRateProviderService rateProviderService;

    @BeforeEach
    void mockRates() {
        when(rateProviderService.fetchRate(anyString(), anyString()))
                .thenAnswer(invocation -> new FxRate(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        new BigDecimal("1.5"),
                        LocalDate.now(),
                        "mock"
                ));
    }

    @Test
    void customerCanCompleteSuccessfulWorkflowAndRetrySubmissionIdempotently()
            throws Exception {

        String customer = createUser("CUSTOMER");
        String operations = createUser("OPERATIONS");
        String beneficiaryId = createBeneficiary(
                customer, "US", "USD"
        );
        String quoteId = createQuote(customer, "USD", "1000.00");

        MvcResult draftResult = mockMvc.perform(post("/api/v1/transfers")
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "beneficiaryId": "%s",
                                  "quoteId": "%s"
                                }
                                """.formatted(beneficiaryId, quoteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String transferId = JsonPath.read(
                draftResult.getResponse().getContentAsString(),
                "$.id"
        );

        mockMvc.perform(patch("/api/v1/transfers/{id}", transferId)
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "beneficiaryId": "%s",
                                  "quoteId": "%s"
                                }
                                """.formatted(beneficiaryId, quoteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/transfers/{id}/submit", transferId)
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .header("Idempotency-Key", "submit-success-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/v1/transfers/{id}/submit", transferId)
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .header("Idempotency-Key", "submit-success-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("READY_FOR_PROVIDER"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/dispatch",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.provider").value("mock"));

        mockMvc.perform(get("/api/v1/transfers/{id}", transferId)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(get("/api/v1/transfers/{id}/timeline", transferId)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType", hasItem("TRANSFER_SUBMITTED")))
                .andExpect(jsonPath("$[*].eventType", hasItem("REVIEW_STARTED")))
                .andExpect(jsonPath("$[*].eventType", hasItem("TRANSFER_APPROVED")))
                .andExpect(jsonPath("$[*].eventType", hasItem("PROVIDER_STATUS_CONFIRMED")));
    }

    @Test
    void complianceHoldBlocksDispatchUntilComplianceClearsIt() throws Exception {
        String customer = createUser("CUSTOMER");
        String operations = createUser("OPERATIONS");
        String compliance = createUser("COMPLIANCE");
        String admin = createUser("ADMIN");

        String beneficiaryId = createBeneficiary(customer, "NZ", "NZD");
        String quoteId = createQuote(customer, "NZD", "1000.00");
        String transferId = createSubmittedTransfer(
                customer,
                beneficiaryId,
                quoteId,
                "hold-" + UUID.randomUUID()
        );

        mockMvc.perform(get("/api/v1/transfers/{id}", transferId)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLIANCE_HOLD"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString(
                        "UNDER_REVIEW"
                )));

        mockMvc.perform(post(
                        "/api/v1/compliance/transfers/{id}/clear-hold",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Should not be allowed\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(
                        "/api/v1/compliance/transfers/{id}/clear-hold",
                        transferId
                )
                        .with(user(admin).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Admin cannot clear\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(
                        "/api/v1/compliance/transfers/{id}/clear-hold",
                        transferId
                )
                        .with(user(compliance).roles("COMPLIANCE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/v1/compliance/transfers/{id}/clear-hold",
                        transferId
                )
                        .with(user(compliance).roles("COMPLIANCE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Simulated screening completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdStatus").value("CLEARED"))
                .andExpect(jsonPath("$.transferStatus").value("UNDER_REVIEW"));

        mockMvc.perform(get(
                        "/api/v1/compliance/transfers/{id}/decisions",
                        transferId
                )
                        .with(user(compliance).roles("COMPLIANCE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.decisionType == 'HOLD_CLEARED')].reason"
                ).value("Simulated screening completed"))
                .andExpect(jsonPath(
                        "$[?(@.decisionType == 'HOLD_CLEARED')].actorRole"
                ).value("COMPLIANCE"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_PROVIDER"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/dispatch",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void providerFailureAndUnknownInquiryOutcomesAreDeterministic()
            throws Exception {
        String customer = createUser("CUSTOMER");
        String operations = createUser("OPERATIONS");

        String gbBeneficiary = createBeneficiary(customer, "GB", "GBP");
        String gbQuote = createQuote(customer, "GBP", "1000.00");
        String gbTransfer = createSubmittedTransfer(
                customer,
                gbBeneficiary,
                gbQuote,
                "failure-" + UUID.randomUUID()
        );

        readyAndDispatch(operations, gbTransfer);

        mockMvc.perform(get("/api/v1/transfers/{id}", gbTransfer)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        String jpBeneficiary = createBeneficiary(customer, "JP", "JPY");
        String jpQuote = createQuote(customer, "JPY", "1000.00");
        String jpTransfer = createSubmittedTransfer(
                customer,
                jpBeneficiary,
                jpQuote,
                "unknown-" + UUID.randomUUID()
        );

        readyAndDispatch(operations, jpTransfer);

        mockMvc.perform(get("/api/v1/transfers/{id}", jpTransfer)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/provider-status-inquiry",
                        jpTransfer
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.inquiries[0].result").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/transfers/{id}", jpTransfer)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void reusingAnIdempotencyKeyForAnotherTransferIsRejected()
            throws Exception {
        String customer = createUser("CUSTOMER");

        String beneficiaryId = createBeneficiary(customer, "US", "USD");
        String quoteOne = createQuote(customer, "USD", "1000.00");
        String quoteTwo = createQuote(customer, "USD", "1000.00");

        String firstTransfer = createDraft(
                customer, beneficiaryId, quoteOne
        );
        String secondTransfer = createDraft(
                customer, beneficiaryId, quoteTwo
        );

        mockMvc.perform(post(
                        "/api/v1/transfers/{id}/submit",
                        firstTransfer
                )
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .header("Idempotency-Key", "shared-key"))
                .andExpect(status().isOk());

        mockMvc.perform(post(
                        "/api/v1/transfers/{id}/submit",
                        secondTransfer
                )
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .header("Idempotency-Key", "shared-key"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString(
                        "different request data"
                )));
    }

    @Test
    void customerCannotReadAnotherCustomersTransferOrOperationsQueue()
            throws Exception {
        String customerOne = createUser("CUSTOMER");
        String customerTwo = createUser("CUSTOMER");

        String beneficiaryId = createBeneficiary(
                customerOne, "US", "USD"
        );
        String quoteId = createQuote(customerOne, "USD", "1000.00");
        String transferId = createDraft(
                customerOne, beneficiaryId, quoteId
        );

        mockMvc.perform(get("/api/v1/transfers/{id}", transferId)
                        .with(user(customerTwo).roles("CUSTOMER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/operations/queue")
                        .with(user(customerOne).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void operationsCanRequestInformationAndCustomerCanRespond() throws Exception {
        String customer = createUser("CUSTOMER");
        String operations = createUser("OPERATIONS");
        String beneficiaryId = createBeneficiary(customer, "US", "USD");
        String quoteId = createQuote(customer, "USD", "1000.00");
        String transferId = createSubmittedTransfer(
                customer,
                beneficiaryId,
                quoteId,
                "info-" + UUID.randomUUID()
        );

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/request-info",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Please confirm the source of funds.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_INFO"));

        mockMvc.perform(get("/api/v1/operations/queue")
                        .param("status", "NEEDS_INFO")
                        .with(user(operations).roles("OPERATIONS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(transferId)));

        mockMvc.perform(post(
                        "/api/v1/transfers/{id}/respond",
                        transferId
                )
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/v1/transfers/{id}/respond",
                        transferId
                )
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Salary savings, evidence supplied.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(get(
                        "/api/v1/operations/transfers/{id}/information-requests",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].responseMessage")
                        .value("Salary savings, evidence supplied."))
                .andExpect(jsonPath("$[0].respondedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/transfers/{id}/timeline", transferId)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType", hasItem(
                        "INFORMATION_REQUESTED"
                )))
                .andExpect(jsonPath("$[*].eventType", hasItem(
                        "INFORMATION_SUBMITTED"
                )))
                .andExpect(jsonPath("$[*].eventType", hasItem(
                        "REVIEW_STARTED"
                )));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_PROVIDER"));
    }

    @Test
    void operationsRejectionIsTerminalAndRequiresAReason() throws Exception {
        String customer = createUser("CUSTOMER");
        String operations = createUser("OPERATIONS");
        String beneficiaryId = createBeneficiary(customer, "US", "USD");
        String quoteId = createQuote(customer, "USD", "1000.00");
        String transferId = createSubmittedTransfer(
                customer,
                beneficiaryId,
                quoteId,
                "reject-" + UUID.randomUUID()
        );

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/reject",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/reject",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Inconsistent supporting details.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/transfers/{id}/timeline", transferId)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType == 'TRANSFER_REJECTED')]"
                        + ".previousStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$[?(@.eventType == 'TRANSFER_REJECTED')]"
                        + ".resultingStatus").value("REJECTED"));

        TransferAuditEvent rejection = auditRepository
                .findAllByTransferIdOrderByCreatedAtAsc(
                        UUID.fromString(transferId)
                )
                .stream()
                .filter(event -> event.getEventType()
                        == TransferEventType.TRANSFER_REJECTED)
                .findFirst()
                .orElseThrow();

        assertEquals("Inconsistent supporting details.", rejection.getReason());
        assertEquals("OPERATIONS", rejection.getActorRole());
        assertEquals(TransferStatus.UNDER_REVIEW,
                rejection.getPreviousStatus());
        assertEquals(TransferStatus.REJECTED,
                rejection.getResultingStatus());
        assertNotNull(rejection.getActorId());
        assertNotNull(rejection.getCreatedAt());
        assertTrue(rejection.getCreatedAt().isBefore(
                java.time.Instant.now().plusSeconds(1)
        ));
    }

    @Test
    void invalidTransitionsAndProviderRetriesAreRejectedWithoutStateChange()
            throws Exception {
        String customer = createUser("CUSTOMER");
        String operations = createUser("OPERATIONS");
        String beneficiaryId = createBeneficiary(customer, "US", "USD");
        String quoteId = createQuote(customer, "USD", "1000.00");
        String transferId = createSubmittedTransfer(
                customer,
                beneficiaryId,
                quoteId,
                "invalid-" + UUID.randomUUID()
        );

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/dispatch",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/request-info",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Please clarify the purpose.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_INFO"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/reject",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cannot reject while awaiting info.\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post(
                        "/api/v1/transfers/{id}/respond",
                        transferId
                )
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Purpose is family support.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        String firstAttemptId = readyAndDispatch(operations, transferId);

        mockMvc.perform(get("/api/v1/transfers/{id}", transferId)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/dispatch",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(firstAttemptId));

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/provider-status-inquiry",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/transfers/{id}", transferId)
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void roleBoundariesAreEnforcedOnEveryWorkspace() throws Exception {
        String customer = createUser("CUSTOMER");
        String operations = createUser("OPERATIONS");
        String compliance = createUser("COMPLIANCE");
        String admin = createUser("ADMIN");

        String beneficiaryId = createBeneficiary(customer, "US", "USD");
        String quoteId = createQuote(customer, "USD", "1000.00");
        String transferId = createSubmittedTransfer(
                customer,
                beneficiaryId,
                quoteId,
                "rbac-" + UUID.randomUUID()
        );

        mockMvc.perform(get("/api/v1/operations/queue")
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/compliance/queue")
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user(customer).roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/compliance/queue")
                        .with(user(operations).roles("OPERATIONS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user(operations).roles("OPERATIONS")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/operations/queue")
                        .with(user(compliance).roles("COMPLIANCE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user(compliance).roles("COMPLIANCE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/operations/queue")
                        .with(user(admin).roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/compliance/queue")
                        .with(user(admin).roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(admin).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/transfers/{id}", transferId)
                        .with(user(operations).roles("OPERATIONS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/transfers/{id}", transferId)
                        .with(user(compliance).roles("COMPLIANCE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user(admin).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    private String readyAndDispatch(String operations, String transferId)
            throws Exception {
        mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/mark-ready",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk());

        MvcResult dispatchResult = mockMvc.perform(post(
                        "/api/v1/operations/transfers/{id}/dispatch",
                        transferId
                )
                        .with(user(operations).roles("OPERATIONS"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(
                dispatchResult.getResponse().getContentAsString(),
                "$.attemptId"
        );
    }

    private String createUser(String role) {
        String email = role.toLowerCase() + "-" + UUID.randomUUID()
                + "@example.test";

        userService.createUser(
                email,
                "TestPassword123!",
                "Test " + role,
                role
        );

        return email;
    }

    private String createBeneficiary(
            String customer,
            String country,
            String currency
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/beneficiaries")
                                .with(user(customer).roles("CUSTOMER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "recipientName": "Alex Morgan",
                                          "destinationCountry": "%s",
                                          "destinationCurrency": "%s",
                                          "accountReference": "DEMO-ACCOUNT-001"
                                        }
                                        """.formatted(country, currency)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
    }

    private String createQuote(
            String customer,
            String destinationCurrency,
            String amount
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/fx/quotes")
                                .with(user(customer).roles("CUSTOMER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sourceCurrency": "INR",
                                          "destinationCurrency": "%s",
                                          "sourceAmount": %s
                                        }
                                        """.formatted(
                                                destinationCurrency,
                                                amount
                                        )))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.quoteId"
        );
    }

    private String createDraft(
            String customer,
            String beneficiaryId,
            String quoteId
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/transfers")
                                .with(user(customer).roles("CUSTOMER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "beneficiaryId": "%s",
                                          "quoteId": "%s"
                                        }
                                        """.formatted(
                                                beneficiaryId,
                                                quoteId
                                        )))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
    }

    private String createSubmittedTransfer(
            String customer,
            String beneficiaryId,
            String quoteId,
            String idempotencyKey
    ) throws Exception {
        String transferId = createDraft(
                customer, beneficiaryId, quoteId
        );

        mockMvc.perform(post(
                        "/api/v1/transfers/{id}/submit",
                        transferId
                )
                        .with(user(customer).roles("CUSTOMER"))
                        .with(csrf())
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk());

        return transferId;
    }
}
