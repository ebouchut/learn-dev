package com.ericbouchut.learndev.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Sends the password-reset email. In development the message is caught by
 * Mailpit (web UI: http://localhost:8025); nothing leaves the machine.
 */
@Component
public class PasswordResetMailer {

    private final JavaMailSender mailSender;
    private final String from;
    private final Duration tokenTtl;

    public PasswordResetMailer(JavaMailSender mailSender,
                               @Value("${learndev.mail.from}") String from,
                               @Value("${learndev.password-reset.token-ttl}") Duration tokenTtl) {
        this.mailSender = mailSender;
        this.from = from;
        this.tokenTtl = tokenTtl;
    }

    /**
     * @param to        recipient email address
     * @param resetLink absolute URL containing the RAW token; the raw token
     *                  exists only in this email and in the URL bar, never
     *                  in the database or the logs
     */
    public void sendResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset your learn-dev password");
        message.setText("""
                Someone (hopefully you) asked to reset the password of the \
                learn-dev account linked to this address.

                To choose a new password, open this link (valid for %d minutes, single use):

                %s

                If you did not ask for this, you can safely ignore this email; \
                your password is unchanged.
                """.formatted(tokenTtl.toMinutes(), resetLink));
        mailSender.send(message);
    }
}
