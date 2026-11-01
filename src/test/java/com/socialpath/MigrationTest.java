package com.socialpath;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.UUID;

/**
 * Base for tests of migrations that change existing data: each test gets an
 * empty database of its own on the shared test MySQL, migrates it to the
 * version before, writes rows that schema allows, then runs the migration
 * under test.
 */
abstract class MigrationTest {

    private SingleConnectionDataSource dataSource;
    protected JdbcTemplate jdbc;

    @BeforeEach
    void emptyDatabase() {
        String database = "migration_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String server = MySqlRepositoryTest.MYSQL.getJdbcUrl()
                .replaceFirst("/" + MySqlRepositoryTest.MYSQL.getDatabaseName(), "/");
        // The test user may not create databases; root can (Testcontainers gives it the same password).
        new JdbcTemplate(new DriverManagerDataSource(server, "root", MySqlRepositoryTest.MYSQL.getPassword()))
                .execute("CREATE DATABASE " + database);
        // One connection throughout: LAST_INSERT_ID() is per connection.
        dataSource = new SingleConnectionDataSource(server + database, "root", MySqlRepositoryTest.MYSQL.getPassword(), true);
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void closeConnection() {
        dataSource.destroy();
    }

    protected void migrateTo(String version) {
        Flyway.configure().dataSource(dataSource).target(version).load().migrate();
    }
}
