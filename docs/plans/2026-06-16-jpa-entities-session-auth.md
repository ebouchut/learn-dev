# JPA Entities + Session Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the JPA persistence layer for `users`/`roles` and a working Spring Security **session-based** login + registration on top of the existing Liquibase schema.

**Architecture:** Feature-based packages under `com.ericbouchut.learndev` (`user`, `role`, `auth`, `common`). Entities map to the existing tables (Hibernate `ddl-auto: validate`, so mappings must match the DB exactly). Authentication is server-side session form login (ADR-0001), passwords hashed with BCrypt, default role `STUDENT` assigned at registration. Repository tests use Testcontainers PostgreSQL (real Postgres + Liquibase), so they match production types (UUID, timestamptz).

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Thymeleaf, Lombok, Liquibase, Testcontainers, PostgreSQL 17.

**Scope:** `User` + `Role` entities, repositories, `UserDetailsService`, `SecurityConfig`, registration, login/logout, one protected page. **Out of scope** (deferred to their features): `email_tokens`/`reset_tokens` entities (password-reset epic #51–#56), `audit_logs` entity (audit feature), account-lockout counting logic, the `user_roles` extra columns `assigned_at`/`assigned_by` (a plain `@ManyToMany` is used; DB defaults populate `assigned_at`).

---

## Version Control (GitButler — applies to every "Commit" step)

This repository is on the `gitbutler/workspace` branch, so **use GitButler (`but`), not raw `git`** (per CLAUDE.md):

- Each task's **Commit** step shows a `git commit -m "<msg>"` for readability. **Execute it as `but commit -m "<msg>"`** instead (GitButler auto-stages the worktree changes). Do **not** run `git add` / `git commit`.
- **NEVER push** — no `but push`, no `git push`. The user reviews in GitButler and pushes manually.
- All non-VCS verifications (`mvn …`, `docker …`) are unchanged.

---

## File Structure

```
src/main/java/com/ericbouchut/learndev/
├── role/
│   ├── entity/Role.java                 # maps roles table
│   └── repository/RoleRepository.java
├── user/
│   ├── entity/User.java                 # maps users table (+ @ManyToMany roles)
│   └── repository/UserRepository.java
├── auth/
│   ├── CustomUserDetailsService.java    # loads User for Spring Security
│   ├── RegistrationService.java         # create account, hash pwd, default role
│   ├── AuthController.java              # GET /register, POST /register, GET /login
│   ├── dto/RegisterForm.java            # validated form-backing record
│   └── exception/
│       ├── DuplicateUsernameException.java
│       └── DuplicateEmailException.java
└── common/config/SecurityConfig.java    # filter chain, PasswordEncoder

src/main/resources/
├── templates/{home,login,register,dashboard}.html
└── db/changelog/changes/V20260616090000-seed-roles.sql

src/test/java/com/ericbouchut/learndev/
├── support/AbstractPostgresIT.java      # Testcontainers base
├── role/repository/RoleRepositoryTest.java
├── user/repository/UserRepositoryTest.java
├── auth/RegistrationServiceTest.java
└── auth/AuthFlowIT.java                 # end-to-end MockMvc
```

---

## Task 1: Add dependencies (validation + Testcontainers)

**Files:**
- Modify: `pom.xml` (inside `<dependencies>`)

- [ ] **Step 1: Add the dependencies**

Add these inside `<dependencies>` in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Verify resolution**

Run: `mvn -q dependency:resolve`
Expected: BUILD SUCCESS (versions come from the Spring Boot parent BOM; no explicit versions needed).

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore(deps): add validation and Testcontainers for auth feature"
```

---

## Task 2: Testcontainers base class for repository tests

**Files:**
- Create: `src/test/java/com/ericbouchut/learndev/support/AbstractPostgresIT.java`

- [ ] **Step 1: Write the base class**

```java
package com.ericbouchut.learndev.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for tests that need a real PostgreSQL (UUID, timestamptz, Liquibase).
 * The container is started once and shared (static); @ServiceConnection wires
 * Spring Boot's datasource to it automatically.
 */
@Testcontainers
public abstract class AbstractPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/java/com/ericbouchut/learndev/support/AbstractPostgresIT.java
git commit -m "test: add Testcontainers PostgreSQL base class"
```

---

## Task 3: Role entity + repository

**Files:**
- Create: `src/main/java/com/ericbouchut/learndev/role/entity/Role.java`
- Create: `src/main/java/com/ericbouchut/learndev/role/repository/RoleRepository.java`
- Test: `src/test/java/com/ericbouchut/learndev/role/repository/RoleRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ericbouchut.learndev.role.repository;

import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoleRepositoryTest extends AbstractPostgresIT {

    @Autowired
    RoleRepository roles;

    @Test
    void seeded_STUDENT_role_is_found_by_name() {
        Optional<Role> student = roles.findByRoleName("STUDENT");
        assertThat(student).isPresent();
        assertThat(student.get().getRoleId()).isNotNull();
    }
}
```

> Note: the seed rows come from the Liquibase migration created in Task 5; running
> this test before Task 5 fails on the assertion, which is the expected red state.

- [ ] **Step 2: Write the entity**

```java
package com.ericbouchut.learndev.role.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
```

- [ ] **Step 3: Write the repository**

```java
package com.ericbouchut.learndev.role.repository;

import com.ericbouchut.learndev.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
}
```

- [ ] **Step 4: Run the test (will pass after Task 5 seeds roles)**

Run: `mvn -q -Dtest=RoleRepositoryTest test`
Expected after Task 5: PASS. (If run now: FAIL on `isPresent()` — proceed to Task 5.)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ericbouchut/learndev/role
git add src/test/java/com/ericbouchut/learndev/role
git commit -m "feat(role): add Role entity and repository"
```

---

## Task 4: User entity + repository

**Files:**
- Create: `src/main/java/com/ericbouchut/learndev/user/entity/User.java`
- Create: `src/main/java/com/ericbouchut/learndev/user/repository/UserRepository.java`
- Test: `src/test/java/com/ericbouchut/learndev/user/repository/UserRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ericbouchut.learndev.user.repository;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends AbstractPostgresIT {

    @Autowired
    UserRepository users;

    @Test
    void saves_user_and_generates_uuid_and_finds_by_username() {
        User u = new User();
        u.setUsername("alice");
        u.setEmail("alice@example.com");
        u.setPassword("hashed");
        users.saveAndFlush(u);

        assertThat(u.getUserId()).isNotNull();           // UUID generated
        assertThat(users.findByUsername("alice")).isPresent();
        assertThat(users.existsByEmail("alice@example.com")).isTrue();
        assertThat(users.existsByUsername("bob")).isFalse();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=UserRepositoryTest test`
Expected: FAIL (compilation error: `User` / `UserRepository` do not exist).

- [ ] **Step 3: Write the entity**

```java
package com.ericbouchut.learndev.user.entity;

import com.ericbouchut.learndev.role.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private OffsetDateTime passwordChangedAt;

    // Plain many-to-many: Hibernate inserts (user_id, role_id); the extra
    // user_roles columns (assigned_at) are populated by their DB defaults.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
```

- [ ] **Step 4: Write the repository**

```java
package com.ericbouchut.learndev.user.repository;

import com.ericbouchut.learndev.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -Dtest=UserRepositoryTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ericbouchut/learndev/user
git add src/test/java/com/ericbouchut/learndev/user
git commit -m "feat(user): add User entity and repository"
```

---

## Task 5: Seed the roles (Liquibase migration)

**Files:**
- Create: `src/main/resources/db/changelog/changes/V20260616090000-seed-roles.sql`

- [ ] **Step 1: Write the migration**

```sql
--liquibase formatted sql

-- Seed the fixed set of application roles.
--changeset ebouchut:V20260616090000
INSERT INTO roles (role_name, description) VALUES
    ('STUDENT',    'Learner who follows courses and does exercises'),
    ('INSTRUCTOR', 'Author of courses, lessons and exercises'),
    ('ADMIN',      'Platform administrator');
--rollback DELETE FROM roles WHERE role_name IN ('STUDENT', 'INSTRUCTOR', 'ADMIN');
```

- [ ] **Step 2: Apply to the running dev DB and verify**

Run:
```bash
docker compose up -d
mvn -q spring-boot:run &   # starts app, Liquibase applies the seed; Ctrl-C after "Started LearnDevApplication"
```
Then:
```bash
docker exec learn-dev-postgres-1 psql -U postgres -d learndev -At -c "SELECT role_name FROM roles ORDER BY role_name;"
```
Expected: `ADMIN`, `INSTRUCTOR`, `STUDENT`.

- [ ] **Step 3: Run the Role test (now green)**

Run: `mvn -q -Dtest=RoleRepositoryTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/changelog/changes/V20260616090000-seed-roles.sql
git commit -m "feat(role): seed STUDENT, INSTRUCTOR, ADMIN roles"
```

---

## Task 6: Security configuration + password encoder

**Files:**
- Create: `src/main/java/com/ericbouchut/learndev/common/config/SecurityConfig.java`

- [ ] **Step 1: Write the config**

```java
package com.ericbouchut.learndev.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        // CSRF protection is ON by default; Thymeleaf adds the token to <form> automatically.
        return http.build();
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ericbouchut/learndev/common/config/SecurityConfig.java
git commit -m "feat(security): session form-login filter chain and BCrypt encoder"
```

---

## Task 7: CustomUserDetailsService

**Files:**
- Create: `src/main/java/com/ericbouchut/learndev/auth/CustomUserDetailsService.java`
- Test: `src/test/java/com/ericbouchut/learndev/auth/CustomUserDetailsServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(users);

    @Test
    void maps_roles_to_ROLE_authorities() {
        Role student = new Role();
        student.setRoleName("STUDENT");
        User u = new User();
        u.setUsername("alice");
        u.setPassword("hash");
        u.setRoles(Set.of(student));
        when(users.findByUsername("alice")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void throws_when_user_missing() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -Dtest=CustomUserDetailsServiceTest test`
Expected: FAIL (compilation: `CustomUserDetailsService` does not exist).

- [ ] **Step 3: Write the service**

```java
package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRoleName()))
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountLocked(user.isLocked())
                .disabled(!user.isActive())
                .build();
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -q -Dtest=CustomUserDetailsServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ericbouchut/learndev/auth/CustomUserDetailsService.java
git add src/test/java/com/ericbouchut/learndev/auth/CustomUserDetailsServiceTest.java
git commit -m "feat(auth): load users into Spring Security via CustomUserDetailsService"
```

---

## Task 8: Registration service + form DTO + exceptions

**Files:**
- Create: `src/main/java/com/ericbouchut/learndev/auth/dto/RegisterForm.java`
- Create: `src/main/java/com/ericbouchut/learndev/auth/exception/DuplicateUsernameException.java`
- Create: `src/main/java/com/ericbouchut/learndev/auth/exception/DuplicateEmailException.java`
- Create: `src/main/java/com/ericbouchut/learndev/auth/RegistrationService.java`
- Test: `src/test/java/com/ericbouchut/learndev/auth/RegistrationServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.role.repository.RoleRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final RegistrationService service = new RegistrationService(users, roles, encoder);

    @Test
    void hashes_password_and_assigns_STUDENT_role() {
        Role student = new Role();
        student.setRoleName("STUDENT");
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.existsByEmail("alice@example.com")).thenReturn(false);
        when(roles.findByRoleName("STUDENT")).thenReturn(Optional.of(student));
        when(encoder.encode("secret12")).thenReturn("HASHED");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var form = new RegisterForm("alice", "alice@example.com", "secret12");
        User created = service.register(form);

        assertThat(created.getPassword()).isEqualTo("HASHED");
        assertThat(created.getRoles()).extracting(Role::getRoleName).containsExactly("STUDENT");
        verify(users).save(any(User.class));
    }

    @Test
    void rejects_duplicate_email() {
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.existsByEmail("alice@example.com")).thenReturn(true);

        var form = new RegisterForm("alice", "alice@example.com", "secret12");
        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(DuplicateEmailException.class);
        verify(users, never()).save(any());
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -Dtest=RegistrationServiceTest test`
Expected: FAIL (compilation: types do not exist).

- [ ] **Step 3: Write the DTO**

```java
package com.ericbouchut.learndev.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterForm(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password) {
}
```

- [ ] **Step 4: Write the exceptions**

```java
package com.ericbouchut.learndev.auth.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("Username already taken: " + username);
    }
}
```

```java
package com.ericbouchut.learndev.auth.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
```

- [ ] **Step 5: Write the service**

```java
package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.role.repository.RoleRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional
    public User register(RegisterForm form) {
        if (users.existsByUsername(form.username())) {
            throw new DuplicateUsernameException(form.username());
        }
        if (users.existsByEmail(form.email())) {
            throw new DuplicateEmailException(form.email());
        }
        Role student = roles.findByRoleName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not seeded"));

        User user = new User();
        user.setUsername(form.username());
        user.setEmail(form.email());
        user.setPassword(encoder.encode(form.password()));
        user.getRoles().add(student);
        return users.save(user);
    }
}
```

- [ ] **Step 6: Run to verify it passes**

Run: `mvn -q -Dtest=RegistrationServiceTest test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ericbouchut/learndev/auth
git add src/test/java/com/ericbouchut/learndev/auth/RegistrationServiceTest.java
git commit -m "feat(auth): registration service with hashing, default role, duplicate checks"
```

---

## Task 9: Auth controller + Thymeleaf templates

**Files:**
- Create: `src/main/java/com/ericbouchut/learndev/auth/AuthController.java`
- Create: `src/main/resources/templates/home.html`
- Create: `src/main/resources/templates/login.html`
- Create: `src/main/resources/templates/register.html`
- Create: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: Write the controller**

```java
package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final RegistrationService registration;

    public AuthController(RegistrationService registration) {
        this.registration = registration;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm("", "", ""));
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult binding) {
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            registration.register(form);
        } catch (DuplicateUsernameException e) {
            binding.rejectValue("username", "duplicate", "Username already taken");
            return "register";
        } catch (DuplicateEmailException e) {
            binding.rejectValue("email", "duplicate", "Email already registered");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
```

- [ ] **Step 2: Write `home.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>learn-dev</title></head>
<body>
<h1>learn-dev</h1>
<p><a th:href="@{/login}">Login</a> · <a th:href="@{/register}">Register</a></p>
</body>
</html>
```

- [ ] **Step 3: Write `login.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Login</title></head>
<body>
<h1>Login</h1>
<p th:if="${param.registered}">Account created — please log in.</p>
<p th:if="${param.logout}">You have been logged out.</p>
<p th:if="${param.error}" style="color:red">Invalid username or password.</p>
<form th:action="@{/login}" method="post">
    <label>Username <input type="text" name="username" required></label><br>
    <label>Password <input type="password" name="password" required></label><br>
    <button type="submit">Log in</button>
</form>
<p><a th:href="@{/register}">Create an account</a></p>
</body>
</html>
```

- [ ] **Step 4: Write `register.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Register</title></head>
<body>
<h1>Create your account</h1>
<form th:action="@{/register}" th:object="${form}" method="post">
    <label>Username <input type="text" th:field="*{username}"></label>
    <span th:if="${#fields.hasErrors('username')}" th:errors="*{username}" style="color:red"></span><br>
    <label>Email <input type="email" th:field="*{email}"></label>
    <span th:if="${#fields.hasErrors('email')}" th:errors="*{email}" style="color:red"></span><br>
    <label>Password <input type="password" th:field="*{password}"></label>
    <span th:if="${#fields.hasErrors('password')}" th:errors="*{password}" style="color:red"></span><br>
    <button type="submit">Register</button>
</form>
<p><a th:href="@{/login}">Already have an account? Log in</a></p>
</body>
</html>
```

- [ ] **Step 5: Write `dashboard.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head><meta charset="UTF-8"><title>Dashboard</title></head>
<body>
<h1>Dashboard</h1>
<p>Signed in as <strong sec:authentication="name">user</strong>.</p>
<form th:action="@{/logout}" method="post">
    <button type="submit">Log out</button>
</form>
</body>
</html>
```

- [ ] **Step 6: Compile**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ericbouchut/learndev/auth/AuthController.java
git add src/main/resources/templates
git commit -m "feat(auth): registration/login/dashboard pages and controller"
```

---

## Task 10: End-to-end auth flow integration test

**Files:**
- Create: `src/test/java/com/ericbouchut/learndev/auth/AuthFlowIT.java`

- [ ] **Step 1: Write the integration test**

```java
package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        // This feature does not use MongoDB; keep the test context Postgres-only.
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class AuthFlowIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Test
    void register_then_login_then_reach_dashboard() throws Exception {
        // protected page redirects to login when anonymous
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // register
        mvc.perform(post("/register").with(csrf())
                        .param("username", "carol")
                        .param("email", "carol@example.com")
                        .param("password", "secret12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        // wrong password is rejected
        mvc.perform(formLogin("/login").user("carol").password("wrong"))
                .andExpect(unauthenticated());

        // correct login succeeds
        mvc.perform(formLogin("/login").user("carol").password("secret12"))
                .andExpect(authenticated().withUsername("carol"))
                .andExpect(redirectedUrl("/dashboard"));
    }
}
```

- [ ] **Step 2: Run the test**

Run: `mvn -q -Dtest=AuthFlowIT test`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/ericbouchut/learndev/auth/AuthFlowIT.java
git commit -m "test(auth): end-to-end register, login, protected-page flow"
```

---

## Task 11: Harden the session cookie + full verification

**Files:**
- Modify: `src/main/resources/application.yaml` (add under `server:` at the root level)

- [ ] **Step 1: Add session-cookie hardening**

Append to `application.yaml` (top-level key, sibling of `spring:`):

```yaml
server:
  servlet:
    session:
      cookie:
        http-only: true
        same-site: lax
        # secure: true   # enable once served over HTTPS
```

- [ ] **Step 2: Run the whole suite**

Run: `mvn -q test`
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 3: Manual smoke test**

Run:
```bash
docker compose up -d
mvn spring-boot:run
```
Then in a browser: visit `http://localhost:8080/` → Register → log in → land on `/dashboard` → Log out. Confirm `/dashboard` redirects to `/login` when logged out.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yaml
git commit -m "feat(security): harden session cookie (HttpOnly, SameSite=Lax)"
```

---

## Self-Review Notes

- **Spec coverage:** EF-1 (registration) → Tasks 8–9; EF-3 (session login/logout, HttpOnly/SameSite cookie) → Tasks 6, 11; RBAC foundation (roles → authorities) → Tasks 3, 5, 7; persistence layer → Tasks 3–4. ADR-0001 (sessions, not JWT) honored throughout. ADR-0003 (UUID `user_id`) reflected in the `User` mapping.
- **Deferred (not gaps):** `email_tokens`/`reset_tokens` entities (#51–#56), `audit_logs` entity, failed-login lockout counting, `assigned_by` tracking on `user_roles`.
- **Type consistency:** `RegisterForm(username,email,password)` used identically in service, controller, and tests; `findByRoleName`, `findByUsername`, `existsByEmail` names match across repository, service, and tests.
