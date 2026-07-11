package com.ericbouchut.learndev.audit;

import com.ericbouchut.learndev.audit.entity.AuditLog;
import com.ericbouchut.learndev.audit.repository.AuditLogRepository;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.stereotype.Service;

/**
 * Minimal security audit trail: internal use only, no controller.
 * Callers record one event per security-relevant action (see the
 * password-reset flow).
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    /**
     * Records one audit event.
     *
     * @param actionType  machine-readable event name, e.g. {@code PASSWORD_RESET_REQUESTED}
     * @param user        the subject, or {@code null} when there is none
     * @param ipAddress   requester IP, or {@code null}
     * @param successful  whether the action succeeded
     * @param description human-readable detail (never include secrets)
     */
    public void record(String actionType, User user, String ipAddress,
                       boolean successful, String description) {
        AuditLog log = new AuditLog();
        log.setActionType(actionType);
        log.setUser(user);
        log.setIpAddress(ipAddress);
        log.setWasSuccessful(successful);
        log.setDescription(description);
        if (user != null) {
            log.setEntityType("user");
            log.setEntityId(user.getUserId().toString());
        }
        auditLogs.save(log);
    }
}
