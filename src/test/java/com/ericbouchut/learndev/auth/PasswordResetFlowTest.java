package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end password reset loop (issue #56): request a reset, capture the
 * real email through Mailpit's REST API, extract the raw token from the
 * link, consume it, and prove the password changed and the token is
 * single-use.
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
class PasswordResetFlowTest extends AbstractPostgresIT {

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

    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();

    @Test
    void full_reset_loop_changes_the_password_and_burns_the_token() throws Exception {
        // An account to reset.
        mvc.perform(post("/auth/register").with(csrf())
                        .param("username", "dave")
                        .param("email", "dave@example.com")
                        .param("password", "oldpassword1"))
                .andExpect(status().is3xxRedirection());

        // Request the reset: neutral redirect, whatever the outcome.
        mvc.perform(post("/auth/forgot-password").with(csrf())
                        .param("email", "dave@example.com"))
                .andExpect(redirectedUrl("/auth/forgot-password?sent"));

        // The email went through real SMTP into Mailpit; read it back.
        String rawToken = tokenFromLatestEmail();
        assertThat(rawToken).isNotBlank();

        // The emailed link shows the new-password form.
        mvc.perform(get("/auth/reset-password").param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New password")));

        // Consume the token.
        mvc.perform(post("/auth/reset-password").with(csrf())
                        .param("token", rawToken)
                        .param("password", "newpassword2"))
                .andExpect(redirectedUrl("/auth/login?reset"));

        // The old password no longer works; the new one does.
        mvc.perform(formLogin("/auth/login").user("dave").password("oldpassword1"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin("/auth/login").user("dave").password("newpassword2"))
                .andExpect(authenticated().withUsername("dave"));

        // Single use: the same token is now rejected.
        mvc.perform(get("/auth/reset-password").param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("invalid, expired, or has already been used")));
        mvc.perform(post("/auth/reset-password").with(csrf())
                        .param("token", rawToken)
                        .param("password", "anotherpass3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("invalid, expired, or has already been used")));
    }

    /**
     * Polls Mailpit's REST API for the latest message and extracts the raw
     * token from the reset link in its body.
     */
    private String tokenFromLatestEmail() throws Exception {
        String api = "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025);
        String messageId = null;
        for (int attempt = 0; attempt < 20 && messageId == null; attempt++) {
            HttpResponse<String> list = http.send(
                    HttpRequest.newBuilder(URI.create(api + "/api/v1/messages")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode messages = json.readTree(list.body()).path("messages");
            if (!messages.isEmpty()) {
                messageId = messages.get(0).path("ID").asText();
            } else {
                Thread.sleep(250);
            }
        }
        assertThat(messageId).as("Mailpit received the reset email").isNotNull();

        HttpResponse<String> message = http.send(
                HttpRequest.newBuilder(URI.create(api + "/api/v1/message/" + messageId)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String text = json.readTree(message.body()).path("Text").asText();
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(text);
        assertThat(matcher.find()).as("reset link present in the email body").isTrue();
        return matcher.group(1);
    }
}
