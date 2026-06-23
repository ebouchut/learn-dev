package com.ericbouchut.learndev.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that need a real PostgreSQL
 * to test against the real database schema, because the H2 in-memory database
 * does not have some data types such as UUID and TIMESTAMPTZ, and because of Liquibase.
 *
 * <h2>Why a singleton container (and not {@code @Container} / {@code @Testcontainers})</h2>
 * The PostgreSQL container is a JVM-wide singleton: it is started once in a static
 * initializer and shared by every test class that extends this base.
 *
 * We deliberately do NOT use {@code @Testcontainers} + {@code @Container} here.
 * Those tie the container lifecycle to a single test class: the container is
 * stopped after that class finishes. Because this base class is shared by several
 * test classes, the container would be stopped after the first class, and the next
 * class would reuse a dead container, failing with "connection refused" after a
 * 30-second Hikari timeout. The static initializer instead keeps the container
 * alive for the whole test run (the JVM owns the lifecycle); it is reaped when the
 * JVM exits, or by Ryuk in CI.
 *
 * {@link ServiceConnection @ServiceConnection} still wires Spring Boot's
 * {@code DataSource} to the container automatically (JDBC URL, username, password),
 * with no manual {@code @DynamicPropertySource}. It only needs a started container,
 * so it works with the singleton pattern.
 */
public abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }
}
