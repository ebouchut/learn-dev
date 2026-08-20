package com.ericbouchut.learndev.auth.repository;

import com.ericbouchut.learndev.auth.entity.EmailToken;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    /** Lookup by SHA-256 hash (the raw token is never stored). */
    Optional<EmailToken> findByToken(String token);

    /** The user's tokens that were never consumed (to invalidate on resend). */
    List<EmailToken> findByUserAndUsedAtIsNull(User user);
}
