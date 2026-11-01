package com.socialpath;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V10 rewrites the free-text phone numbers into E.164, guessing the country
 * from the profile where the number has no code, and keeps what it cannot
 * read as it was typed.
 */
class PhoneNumberMigrationTest extends MigrationTest {

    private void user(String login, String phone, String countryCode) {
        jdbc.update("""
                INSERT INTO users (login, password, first_name, email, phone_number, country_code)
                VALUES (?, 'hash', 'Test', ?, ?, ?)
                """, login, login + "@example.com", phone, countryCode);
    }

    private Map<String, Object> phoneOf(String login) {
        return jdbc.queryForMap("SELECT phone, phone_legacy, phone_visibility FROM users WHERE login = ?", login);
    }

    @Test
    void oldNumbers_AreReadWithTheirCodeOrTheProfileCountry_TheRestKeptAsTyped() {
        migrateTo("9");
        user("withcode", "+1 201-555-0123", null);
        user("localus", "(201) 555-0124", "US");
        user("nocountry", "555-0100", null);
        user("tooshort", "555-0100", "US");
        user("spaces", "  +1 201 555 0125  ", null);

        migrateTo("10");

        assertEquals("+12015550123", phoneOf("withcode").get("phone"));
        assertEquals("+12015550124", phoneOf("localus").get("phone"));
        assertEquals("+12015550125", phoneOf("spaces").get("phone"));
        // No code and no country, or not a whole number for the country: kept as typed.
        assertEquals(null, phoneOf("nocountry").get("phone"));
        assertEquals("555-0100", phoneOf("nocountry").get("phone_legacy"));
        assertEquals("555-0100", phoneOf("tooshort").get("phone_legacy"));
        assertEquals(null, phoneOf("withcode").get("phone_legacy"));
        // Nobody's number becomes public by the migration.
        assertEquals("FRIENDS", phoneOf("withcode").get("phone_visibility"));
    }

    @Test
    void afterwards_EveryUserStillHasANumber() {
        migrateTo("10");

        DataAccessException refused = assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO users (login, password, first_name, email) VALUES ('nophone', 'hash', 'Test', 'n@example.com')
                """));
        assertTrue(refused.getMessage().contains("chk_users_phone"), refused.getMessage());
    }
}
