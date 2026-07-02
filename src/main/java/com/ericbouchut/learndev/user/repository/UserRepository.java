package com.ericbouchut.learndev.user.repository;

import com.ericbouchut.learndev.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Loads a user with their roles fetched in the same query. Roles are LAZY on
     * the entity; authentication is the one path that always needs them, so this
     * finder opts in via an entity graph (a single join, no lazy-init risk).
     */
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
