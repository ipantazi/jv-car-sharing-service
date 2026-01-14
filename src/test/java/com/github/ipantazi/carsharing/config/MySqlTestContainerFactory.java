package com.github.ipantazi.carsharing.config;

import java.time.Duration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@SuppressWarnings("resource")
public class MySqlTestContainerFactory {
    private static final MySQLContainer<?> CONTAINER;

    static {
        CONTAINER = new MySQLContainer<>(
                System.getenv().getOrDefault("TEST_DB_IMAGE", "mysql:8.0.43")
        )
                .withDatabaseName(getEnv("TEST_DB_NAME", "testdb"))
                .withUsername(getEnv("TEST_DB_USER", "test"))
                .withPassword(getEnv("TEST_DB_PASSWORD", "test"))
                .withReuse(true)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));

        CONTAINER.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (CONTAINER.isRunning()) {
                CONTAINER.stop();
            }
        }));
    }

    private MySqlTestContainerFactory() {
    }

    public static MySQLContainer<?> getInstance() {
        return CONTAINER;
    }

    private static String getEnv(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }
}
