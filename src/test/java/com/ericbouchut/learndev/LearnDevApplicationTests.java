package com.ericbouchut.learndev;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * This smoke test verifies the Spring application context starts.
 * <br/>
 * It runs against a real PostgreSQL database started in a container (via
 * {@link AbstractPostgresIT}) instead of the dev database that may be down,
 * so it is self-contained.
 * <br>
 * MongoDB is not used by this feature and is not running in
 * tests, so its auto-configuration is excluded.
 */
@SpringBootTest(properties =
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration")
class LearnDevApplicationTests extends AbstractPostgresIT {

    @Test
    void contextLoads() {
    }

}
