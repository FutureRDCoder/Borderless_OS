package com.borderlessos.common;

import com.borderlessos.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasLength;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingRequiredHeaderShouldReturnBadRequest() throws Exception {
        UUID transferId = UUID.randomUUID();

        mockMvc.perform(post(
                        "/api/v1/transfers/{id}/submit",
                        transferId
                )
                        .with(user("customer@example.test").roles("CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"))
                .andExpect(jsonPath("$.message", containsString(
                        "Idempotency-Key"
                )))
                .andExpect(jsonPath("$.path").value(
                        "/api/v1/transfers/" + transferId + "/submit"
                ));
    }

    @Test
    void unsupportedHttpMethodShouldReturnMethodNotAllowed()
            throws Exception {
        mockMvc.perform(post("/api/v1/admin/dashboard")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedContentTypeShouldReturnUnsupportedMediaType()
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value(
                        "UNSUPPORTED_MEDIA_TYPE"
                ));
    }

    @Test
    void securityErrorsShouldEchoRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-Request-Id", "request-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "request-123"))
                .andExpect(jsonPath("$.requestId").value("request-123"));
    }

    @Test
    void securityErrorsShouldTruncateOversizedRequestId() throws Exception {
        String oversized = "z".repeat(200);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-Request-Id", oversized))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", hasLength(128)))
                .andExpect(jsonPath("$.requestId", hasLength(128)));
    }

    @Test
    void loginValidationErrorsShouldUseStandardErrorEnvelope()
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " ",
                                  "password": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "Email and password are required."
                ))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void loginAuthenticationErrorsShouldUseStandardErrorEnvelope()
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nobody@example.test",
                                  "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value(
                        "Invalid email or password"
                ))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.requestId").exists());
    }
}
