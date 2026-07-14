package com.ericbouchut.learndev.auth.entity;

import com.ericbouchut.learndev.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * A single-use, expiring email-verification token (maps the
 * {@code email_tokens} table). Like the password-reset token, the
 * {@code token} column stores the SHA-256 <b>hash</b> of the raw secret sent
 * by email, never the secret itself.
 */
@Entity
@Table(name = "email_tokens")
@Getter
@Setter
@NoArgsConstructor
public class EmailToken {

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** True when the token can still be consumed (not used, not expired). */
    public boolean isUsable(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
