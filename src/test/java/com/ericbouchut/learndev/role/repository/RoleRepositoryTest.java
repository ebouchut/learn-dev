package com.ericbouchut.learndev.role.repository;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * <code>@AutoConfigureTestDatabase(Replace = NONE</code> prevents
 * replacement with an embedded database (H2).
 * We keep the Testcontainers Postgres via {@link AbstractPostgresIT}
 * because we need to test against the real DB schema and datatypes that H2 does not support.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoleRepositoryTest extends AbstractPostgresIT {

    @Autowired
    RoleRepository roleRepository;

    @Test
    void finds_a_seeded_role_by_its_name() {
        // Arrange (Given): roles are a fixed set seeded by Liquibase (V*-seed-roles.sql)

        // Act (When): look up a seeded role by name
        var maybeRole = roleRepository.findByRoleName("STUDENT");

        // Assert (Then): Verify the result
        assertThat(maybeRole).isPresent();
        assertThat(maybeRole.get().getRoleId()).isNotNull();   // BIGINT identity from the DB
        assertThat(maybeRole.get().isActive()).isTrue();        // is_active defaults to true
    }
}
