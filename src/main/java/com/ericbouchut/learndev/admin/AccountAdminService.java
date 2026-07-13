package com.ericbouchut.learndev.admin;

import com.ericbouchut.learndev.audit.AuditService;
import com.ericbouchut.learndev.auth.RegistrationService;
import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Account administration: create instructor accounts and archive or
 * reactivate any account. Archiving means {@code is_active = false}, which
 * Spring Security already maps to a disabled login (v1 has no destructive
 * delete); an admin cannot archive their own account (409), so the platform
 * cannot lock every admin out. The initial instructor password is set by the
 * admin and handed over out of band; the instructor can change it through
 * the existing password-reset flow. Every action lands in the audit trail.
 */
@Service
public class AccountAdminService {

    private final UserRepository users;
    private final RegistrationService registration;
    private final AuditService audit;

    public AccountAdminService(
            UserRepository users,
            RegistrationService registration,
            AuditService audit
    ) {
        this.users = users;
        this.registration = registration;
        this.audit = audit;
    }

    /** Every account, for the admin user list. */
    public List<User> allUsers() {
        return users.findAll(Sort.by("username"));
    }

    /** One account, or 404. */
    public User account(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Create an {@code INSTRUCTOR} account (same uniqueness and hashing
     * rules as self-registration, different seeded role).
     */
    @Transactional
    public User createInstructor(RegisterForm form, User actor, String ipAddress) {
        User created = registration.register(form, "INSTRUCTOR");
        audit.record("ACCOUNT_CREATED", actor, ipAddress, true,
                "Instructor account " + created.getUserId() + " created");
        return created;
    }

    /**
     * Archive an account: the user can no longer log in.
     *
     * @throws ResponseStatusException 409 when the admin targets themselves
     */
    @Transactional
    public void archive(User target, User actor, String ipAddress) {
        if (target.getUserId().equals(actor.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An admin cannot archive their own account");
        }
        target.setActive(false);
        users.save(target);
        audit.record("ACCOUNT_ARCHIVED", actor, ipAddress, true,
                "Account " + target.getUserId() + " archived");
    }

    /** Reactivate an archived account: the user can log in again. */
    @Transactional
    public void reactivate(User target, User actor, String ipAddress) {
        target.setActive(true);
        users.save(target);
        audit.record("ACCOUNT_REACTIVATED", actor, ipAddress, true,
                "Account " + target.getUserId() + " reactivated");
    }
}
