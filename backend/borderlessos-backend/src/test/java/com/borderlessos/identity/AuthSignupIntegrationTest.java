package com.borderlessos.identity;

import com.borderlessos.TestcontainersConfiguration;
import com.borderlessos.identity.entity.User;
import com.borderlessos.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void customerShouldSignUpAndBeSignedInAutomatically() throws Exception {
        String email = uniqueEmail("new-customer");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Aarav Sharma",
                                  "email": "%s",
                                  "password": "StrongPassword123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles").isArray())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[?(@ == 'ROLE_CUSTOMER')]").exists())
                .andExpect(jsonPath("$.roles[?(@ == 'ROLE_ADMIN')]").doesNotExist());

        User user = userRepository.findByEmail(email).orElseThrow();

        assertEquals("Aarav Sharma", user.getDisplayName());
        assertTrue(user.isEnabled());
        assertTrue(user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("CUSTOMER")));
    }

    @Test
    void signupShouldRejectDuplicateEmail() throws Exception {
        String email = uniqueEmail("duplicate");

        String body = """
                {
                  "displayName": "Duplicate User",
                  "email": "%s",
                  "password": "StrongPassword123!"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Email is already registered"));
    }

    @Test
    void signupShouldRejectWeakPasswordAndInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Test User",
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void signupShouldNotAllowPrivilegedRoleSelection() throws Exception {
        String email = uniqueEmail("role-attempt");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Role Attempt",
                                  "email": "%s",
                                  "password": "StrongPassword123!",
                                  "role": "ADMIN"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[?(@ == 'ROLE_ADMIN')]")
                        .doesNotExist());

        User user = userRepository.findByEmail(email).orElseThrow();

        assertEquals(1, user.getRoles().size());
        assertEquals("CUSTOMER", user.getRoles().iterator().next().getName());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }
}
