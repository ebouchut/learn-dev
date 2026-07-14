package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end email verification loop: register, capture the real email
 * through Mailpit's REST API, follow the link, and prove the account is
 * verified and the token single-use; then the resend path from the
 * dashboard banner issues a fresh working link.
 *
 * <p>Runs against the shared Testcontainers PostgreSQL plus a Mailpit
 * container as the SMTP target (the same image used by docker compose).
 *
 * <p>Named with the {@code Test} suffix (not {@code IT}) so Surefire runs it
 * as part of {@code mvn test}; this project does not use the Failsafe plugin.
 */
@SpringBootTest(properties = {
        // This feature does not use MongoDB; keep the test context Postgres-only.
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
@AutoConfigureMockMvc
class EmailVerificationFlowTest extends AbstractPostgresIT {

    /** Shared like the Postgres container: started once for the class. */
    static final GenericContainer<?> MAILPIT =
            new GenericContainer<>("axllent/mailpit")
                    .withExposedPorts(1025, 8025);

    static {
        MAILPIT.start();
    }

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();

    @Test
    void registration_sends_a_single_use_verification_link() throws Exception {
        // Registering triggers the verification email.
        mvc.perform(post("/auth/register").with(csrf())
                        .param("username", "verifyme")
                        .param("email", "verifyme@example.com")
                        .param("password", "secret12"))
                .andExpect(status().is3xxRedirection());
        assertThat(userRepository.findByUsername("verifyme").orElseThrow().isVerified())
                .isFalse();

        // The email went through real SMTP into Mailpit; read it back.
        String rawToken = tokenFromLatestEmail(1);

        // Following the link verifies the account.
        mvc.perform(get("/auth/verify").param("token", rawToken))
                .andExpect(redirectedUrl("/auth/login?verified"));
        assertThat(userRepository.findByUsername("verifyme").orElseThrow().isVerified())
                .isTrue();

        // Single use: the same token is now rejected.
        mvc.perform(get("/auth/verify").param("token", rawToken))
                .andExpect(redirectedUrl("/auth/login?verify-invalid"));
    }

    @Test
    void the_dashboard_resend_issues_a_fresh_working_link() throws Exception {
        mvc.perform(post("/auth/register").with(csrf())
                        .param("username", "resender")
                        .param("email", "resender@example.com")
                        .param("password", "secret12"))
                .andExpect(status().is3xxRedirection());
        String firstToken = tokenFromLatestEmail(1);

        // Resend from the dashboard banner: a new link arrives...
        mvc.perform(post("/auth/verify/resend")
                        .with(user("resender").roles("STUDENT")).with(csrf()))
                .andExpect(redirectedUrl("/dashboard?verification-sent"));
        String secondToken = tokenFromLatestEmail(2);
        assertThat(secondToken).isNotEqualTo(firstToken);

        // ...the first link is invalidated, the fresh one works.
        mvc.perform(get("/auth/verify").param("token", firstToken))
                .andExpect(redirectedUrl("/auth/login?verify-invalid"));
        mvc.perform(get("/auth/verify").param("token", secondToken))
                .andExpect(redirectedUrl("/auth/login?verified"));
    }

    /**
     * Polls Mailpit's REST API for the latest message and extracts the raw
     * token from the verification link in its body.
     */
    private String tokenFromLatestEmail(int expectedCount) throws Exception {
        String api = "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025);
        String text = null;
        for (int attempt = 0; attempt < 20 && text == null; attempt++) {
            HttpResponse<String> list = http.send(
                    HttpRequest.newBuilder(URI.create(api + "/api/v1/messages")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode messages = json.readTree(list.body()).path("messages");
            if (messages.size() >= expectedCount) {
                String id = messages.get(0).path("ID").asText();
                HttpResponse<String> message = http.send(
                        HttpRequest.newBuilder(URI.create(api + "/api/v1/message/" + id))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                text = json.readTree(message.body()).path("Text").asText();
            } else {
                Thread.sleep(250);
            }
        }
        assertThat(text).as("Mailpit received the verification email").isNotNull();
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(text);
        assertThat(matcher.find()).as("verification link present in the email body").isTrue();
        return matcher.group(1);
    }
}
