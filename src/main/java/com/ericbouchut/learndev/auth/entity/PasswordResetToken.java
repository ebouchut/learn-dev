package com.ericbouchut.learndev.auth.entity;

import com.ericbouchut.learndev.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * A single-use, expiring password-reset token (maps the {@code reset_tokens}
 * table). The {@code token} column stores the SHA-256 <b>hash</b> of the raw
 * secret sent by email, never the secret itself: a database leak does not
 * yield usable reset links.
 */
@Entity
@Table(name = "reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    /** SHA-256 hash (hex) of the raw token; unique so lookups are by hash. */
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Set when the token is consumed or invalidated; single-use guard. */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /** Requester's IP (PostgreSQL {@code inet} column). */
    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address")
    private String ipAddress;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** True when the token can still be consumed (not used, not expired). */
    public boolean isUsable(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
