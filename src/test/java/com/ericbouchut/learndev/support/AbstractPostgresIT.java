package com.ericbouchut.learndev.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real PostgreSQL
 * to test against the real database schema because the H2 in-memory database
 * does not have some data types such as UUID, TIMESTAMPTZ, and because of Liquibase.
 *
 * The container is started once and shared (static).
 * {@link ServiceConnection @ServiceConnection} wires Spring Boot's datasource to it
 * automatically.
 */
@Testcontainers
public abstract class AbstractPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");
}
