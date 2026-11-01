package com.socialpath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.mysql.MySQLContainer;

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
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        // Started once for all subclasses; Testcontainers removes it when the JVM exits.
        MYSQL.start();
    }

    @Autowired
    protected JdbcTemplate jdbc;

    /** Inserts a user with only the required columns filled. */
    protected void user(String login) {
        jdbc.update("""
                INSERT INTO users (login, password, first_name, email, phone)
                VALUES (?, 'hash', ?, ?, '+12015550123')
                """, login, login, login + "@example.com");
    }
}
