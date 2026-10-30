package com.socialpath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base for repository tests that run the real SQL against MySQL 8 (the
 * recursive CTEs and ON DELETE CASCADE are MySQL features an in-memory
 * database would not reproduce). The schema comes from the Flyway
 * migrations, exactly as in production. One container serves every test
 * class; each test runs in a transaction that is rolled back afterwards.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class MySqlRepositoryTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        // Started once for all subclasses; Testcontainers removes it when the JVM exits.
        MYSQL.start();
    }

    @Autowired
    protected JdbcTemplate jdbc;

    /** Inserts a user with only the required columns filled. */
    protected void user(String login) {
        jdbc.update("""
                INSERT INTO users (login, password, first_name, email, phone_number)
                VALUES (?, 'hash', ?, ?, '555-0100')
                """, login, login, login + "@example.com");
    }
}
