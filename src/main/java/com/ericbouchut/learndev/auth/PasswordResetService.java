package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.audit.AuditService;
import com.ericbouchut.learndev.auth.entity.PasswordResetToken;
import com.ericbouchut.learndev.auth.repository.PasswordResetTokenRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * The password-reset ("forgot password") flow.
 *
 * <p>Security properties (see issues #53 and #55):
 * <ul>
 *   <li><b>Opaque, high-entropy token</b>: 32 random bytes (256 bits) from
 *       {@link SecureRandom}, sent as base64url in the email link.</li>
 *   <li><b>Hashed at rest</b>: only the SHA-256 of the token is stored; a
 *       database leak yields no usable link.</li>
 *   <li><b>Short-lived and single-use</b>: {@code expires_at} enforces the
 *       TTL, {@code used_at} enforces single use; a new request or a
 *       successful reset invalidates all other outstanding tokens.</li>
 *   <li><b>Enumeration-safe</b>: every outcome of a request (unknown email,
 *       rate-limited, sent) is invisible to the caller; only the audit trail
 *       differs.</li>
 *   <li><b>Rate-limited</b> per user and per IP. The table has no
 *       created_at column, so recency is derived from expires_at:
 *       created &gt; now - window equals expires_at &gt; now - window + ttl.</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final AuditService audit;
    private final Duration tokenTtl;
    private final long maxRequests;
    private final Duration window;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users,
                                PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder,
                                PasswordResetMailer mailer,
                                AuditService audit,
                                @Value("${learndev.password-reset.token-ttl}") Duration tokenTtl,
                                @Value("${learndev.password-reset.max-requests}") long maxRequests,
                                @Value("${learndev.password-reset.window}") Duration window) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.audit = audit;
        this.tokenTtl = tokenTtl;
        this.maxRequests = maxRequests;
        this.window = window;
    }

    /**
     * Handles a "forgot password" request. Always succeeds from the caller's
     * point of view (enumeration-safe): whether the email exists, is
     * rate-limited, or a mail was actually sent is only visible in the audit
     * trail.
     *
     * @param email        address typed in the form
     * @param ipAddress    requester IP (rate limiting and audit)
     * @param resetUrlBase absolute URL of the reset page, without the token,
     *                     e.g. {@code https://host/auth/reset-password}
     */
    @Transactional
    public void requestReset(String email, String ipAddress, String resetUrlBase) {
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            audit.record("PASSWORD_RESET_REQUESTED", null, ipAddress, false,
                    "Reset requested for an unknown email");
            return;
        }
        User user = found.get();

        // Recency is derived from expires_at (no created_at column).
        OffsetDateTime recentCutoff = OffsetDateTime.now().minus(window).plus(tokenTtl);
        if (tokens.countByUserAndExpiresAtAfter(user, recentCutoff) >= maxRequests
                || (ipAddress != null
                    && tokens.countByIpAddressAndExpiresAtAfter(ipAddress, recentCutoff) >= maxRequests)) {
            audit.record("PASSWORD_RESET_RATE_LIMITED", user, ipAddress, false,
                    "Too many reset requests in the window");
            return;
        }

        // A new request supersedes all outstanding tokens (single active link).
        invalidateOutstandingTokens(user);

        String rawToken = generateRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(sha256(rawToken));
        token.setExpiresAt(OffsetDateTime.now().plus(tokenTtl));
        token.setIpAddress(ipAddress);
        token.setUser(user);
        tokens.save(token);

        try {
            mailer.sendResetEmail(user.getEmail(), resetUrlBase + "?token=" + rawToken);
            audit.record("PASSWORD_RESET_REQUESTED", user, ipAddress, true,
                    "Reset email sent");
        } catch (MailException e) {
            // Keep the response neutral; the failure lives in logs and audit.
            log.error("Could not send the password reset email", e);
            audit.record("PASSWORD_RESET_REQUESTED", user, ipAddress, false,
                    "Reset email could not be sent");
        }
    }

    /**
     * Returns the token entity when the raw token is valid (exists, not
     * expired, not used). Read-only: used by the GET page to decide between
     * the form and the invalid-link message.
     */
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findUsableToken(String rawToken) {
        return tokens.findByToken(sha256(rawToken))
                .filter(t -> t.isUsable(OffsetDateTime.now()));
    }

    /**
     * Consumes the token and sets the new password.
     *
     * @return true on success; false when the token is invalid, expired, or
     *         already used (the caller shows the invalid-link message)
     */
    @Transactional
    public boolean resetPassword(String rawToken, String newPassword, String ipAddress) {
        Optional<PasswordResetToken> found = findUsableToken(rawToken);
        if (found.isEmpty()) {
            audit.record("PASSWORD_RESET_COMPLETED", null, ipAddress, false,
                    "Invalid, expired, or already used token");
            return false;
        }
        PasswordResetToken token = found.get();
        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(OffsetDateTime.now());
        // Completing a reset proves control of the mailbox: clear any
        // failed-login lock along with the counter.
        LoginAttemptListener.unlock(user);
        token.setUsedAt(OffsetDateTime.now());
        // Strict single active link: consuming one kills the others too.
        invalidateOutstandingTokens(user);

        audit.record("PASSWORD_RESET_COMPLETED", user, ipAddress, true,
                "Password changed via reset token");
        return true;
    }

    private void invalidateOutstandingTokens(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        tokens.findByUserAndUsedAtIsNull(user).forEach(t -> t.setUsedAt(now));
    }

    /** 32 random bytes (256 bits), base64url without padding: URL-safe. */
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex of the raw token; what the database stores and looks up. */
    static String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
