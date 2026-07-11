package com.ericbouchut.learndev.auth.repository;

import com.ericbouchut.learndev.auth.entity.PasswordResetToken;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** Lookup by the SHA-256 hash of the raw token. */
    Optional<PasswordResetToken> findByToken(String tokenHash);

    /** Outstanding (not yet consumed) tokens of a user, for invalidation. */
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);

    /**
     * Requests made recently by this user. The table has no created_at
     * column, so recency is derived from expires_at (creation + TTL): a
     * token created within the last {@code window} has
     * {@code expires_at > now - window + ttl}.
     */
    long countByUserAndExpiresAtAfter(User user, OffsetDateTime expiresAfter);

    /** Same recency approximation, keyed by the requester's IP. */
    long countByIpAddressAndExpiresAtAfter(String ipAddress, OffsetDateTime expiresAfter);
}
