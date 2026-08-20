package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.audit.AuditService;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Account lockout: counts consecutive failed logins per account and locks
 * the account at the configured threshold
 * ({@code learndev.lockout.max-attempts}). A successful login resets the
 * counter and stamps {@code last_login_at}. {@code is_locked} is already
 * mapped to Spring Security's {@code accountLocked}, so a locked account is
 * rejected at authentication time. Unlocking happens through the admin area
 * or by completing a password reset (which proves control of the mailbox).
 * Failures for unknown usernames are ignored: no row, no counter, and no
 * behavior difference that could leak whether an account exists.
 */
@Component
public class LoginAttemptListener {

    private final UserRepository users;
    private final AuditService audit;
    private final int maxAttempts;

    public LoginAttemptListener(
            UserRepository users,
            AuditService audit,
            @Value("${learndev.lockout.max-attempts:5}") int maxAttempts
    ) {
        this.users = users;
        this.audit = audit;
        this.maxAttempts = maxAttempts;
    }

    /** Wrong password: count it, and lock the account at the threshold. */
    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        users.findByUsername(username).ifPresent(user -> {
            if (user.isLocked()) {
                return;
            }
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxAttempts) {
                user.setLocked(true);
                audit.record("ACCOUNT_LOCKED", user, null, true,
                        "Locked after " + user.getFailedLoginAttempts()
                                + " failed login attempts");
            }
            users.save(user);
        });
    }

    /** Successful login: reset the counter and stamp the login time. */
    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        users.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(OffsetDateTime.now());
            users.save(user);
        });
    }

    /** Clear the lock and the counter (admin unlock, password reset). */
    public static void unlock(User user) {
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
    }
}
