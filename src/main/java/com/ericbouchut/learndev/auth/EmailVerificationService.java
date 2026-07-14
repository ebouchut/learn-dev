package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.audit.AuditService;
import com.ericbouchut.learndev.auth.entity.EmailToken;
import com.ericbouchut.learndev.auth.repository.EmailTokenRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * Email verification: proves the registered address belongs to the user.
 * Same token discipline as the password reset: a 32-byte random secret sent
 * by email, stored only as its SHA-256 hash, single active token per user,
 * single use, with a TTL (configurable under
 * {@code learndev.email-verification.*}). Verification is informational in
 * v1 (a dashboard banner until done); it does not gate any feature yet.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailTokenRepository tokens;
    private final UserRepository users;
    private final EmailVerificationMailer mailer;
    private final AuditService audit;
    private final Duration tokenTtl;

    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
            EmailTokenRepository tokens,
            UserRepository users,
            EmailVerificationMailer mailer,
            AuditService audit,
            @Value("${learndev.email-verification.token-ttl}") Duration tokenTtl
    ) {
        this.tokens = tokens;
        this.users = users;
        this.mailer = mailer;
        this.audit = audit;
        this.tokenTtl = tokenTtl;
    }

    /**
     * Issue a fresh token for the user (invalidating any open one) and
     * email the verification link. A mail failure is logged and audited but
     * not surfaced: registration must not fail because SMTP hiccuped.
     *
     * @param user          the account to verify
     * @param ipAddress     requester IP for the audit trail
     * @param verifyUrlBase absolute URL of the verify endpoint, without the token
     */
    @Transactional
    public void sendVerification(User user, String ipAddress, String verifyUrlBase) {
        if (user.isVerified()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        tokens.findByUserAndUsedAtIsNull(user).forEach(open -> {
            open.setUsedAt(now);
            tokens.save(open);
        });

        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);

        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setToken(PasswordResetService.sha256(rawToken));
        token.setExpiresAt(now.plus(tokenTtl));
        tokens.save(token);

        try {
            mailer.sendVerificationEmail(user.getEmail(), verifyUrlBase + "?token=" + rawToken);
            audit.record("EMAIL_VERIFICATION_SENT", user, ipAddress, true,
                    "Verification email sent");
        } catch (MailException e) {
            log.error("Could not send the verification email", e);
            audit.record("EMAIL_VERIFICATION_SENT", user, ipAddress, false,
                    "Verification email could not be sent");
        }
    }

    /**
     * Consume a verification token: marks the user verified and burns the
     * token. Unknown, expired, or already-used tokens simply return false
     * (the caller shows a neutral invalid-link message).
     *
     * @param rawToken  the raw token from the emailed link
     * @param ipAddress requester IP for the audit trail
     * @return true when the email is now verified
     */
    @Transactional
    public boolean verify(String rawToken, String ipAddress) {
        OffsetDateTime now = OffsetDateTime.now();
        return tokens.findByToken(PasswordResetService.sha256(rawToken))
                .filter(token -> token.isUsable(now))
                .map(token -> {
                    token.setUsedAt(now);
                    tokens.save(token);
                    User user = token.getUser();
                    user.setVerified(true);
                    users.save(user);
                    audit.record("EMAIL_VERIFIED", user, ipAddress, true,
                            "Email address verified");
                    return true;
                })
                .orElse(false);
    }
}
