package com.ericbouchut.learndev.user.repository;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest is used for Slice Testing the data layer.
// - It loads only  entities, repositories, `EntityManager`, `DataSource`.
// - It does NOT load controllers, services, security.
// - Tests are transactional and roll back by default
@DataJpaTest
// Do not perform tests against an in-memory H2 database but use the real one
// defined in AbstractPostgresIT.POSTGRES annotated with @ServiceConnection.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends AbstractPostgresIT {

    /**
     * Spring injects the bean: implementation generated from the UserRepository interface.
     */
    @Autowired
    UserRepository userRepository;

    @Test
    void saves_a_user_generates_a_uuid_and_finds_it_by_username() {
        // Arrange (Given): a new user
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("hashed");

        // Act (When): persist it
        userRepository.saveAndFlush(user);

        // Assert (Then): UUID generated and lookups work
        assertThat(user.getUserId()).isNotNull();                          // UUID generated
        assertThat(userRepository.findByUsername("alice")).isPresent();
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
    }
}
