package com.ericbouchut.learndev.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Sends the email-verification message. In development the message is
 * caught by Mailpit (web UI: http://localhost:8025); nothing leaves the
 * machine.
 */
@Component
public class EmailVerificationMailer {

    private final JavaMailSender mailSender;
    private final String from;
    private final Duration tokenTtl;

    public EmailVerificationMailer(JavaMailSender mailSender,
                                   @Value("${learndev.mail.from}") String from,
                                   @Value("${learndev.email-verification.token-ttl}") Duration tokenTtl) {
        this.mailSender = mailSender;
        this.from = from;
        this.tokenTtl = tokenTtl;
    }

    /**
     * @param to         recipient email address
     * @param verifyLink absolute URL containing the RAW token; the raw token
     *                   exists only in this email and in the URL bar, never
     *                   in the database or the logs
     */
    public void sendVerificationEmail(String to, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Verify your learn-dev email address");
        message.setText("""
                Welcome to learn-dev!

                To confirm that this address is yours, open this link \
                (valid for %d hours, single use):

                %s

                If you did not create a learn-dev account, you can safely \
                ignore this email.
                """.formatted(tokenTtl.toHours(), verifyLink));
        mailSender.send(message);
    }
}
