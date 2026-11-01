package db.migration;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Phone numbers move from free text to the international E.164 form, and
 * each user chooses who sees theirs. A Java migration because reading the
 * old numbers takes libphonenumber, which SQL cannot do.
 *
 * <p>An old number with its country code (+...) is read as it is; one without
 * is read as a local number of the country in the user's profile. What still
 * cannot be read (no code and no country, or not a real number) is kept as
 * typed in phone_legacy, so nothing is lost and the user can correct it.
 *
 * <p>The parsing is done here directly rather than through the application's
 * PhoneNumbers class on purpose: a migration must keep doing what it did when
 * it first ran, whatever the application code becomes later.
 */
public class V10__International_phone_numbers extends BaseJavaMigration {

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE users
                        ADD COLUMN phone VARCHAR(16) NULL,
                        ADD COLUMN phone_legacy VARCHAR(20) NULL,
                        ADD COLUMN phone_visibility VARCHAR(10) NOT NULL DEFAULT 'FRIENDS',
                        ADD CONSTRAINT chk_users_phone_visibility
                            CHECK (phone_visibility IN ('EVERYONE', 'FRIENDS', 'NOBODY'))
                    """);
        }

        convertOldNumbers(connection);

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE users
                        DROP COLUMN phone_number,
                        ADD CONSTRAINT chk_users_phone CHECK (phone IS NOT NULL OR phone_legacy IS NOT NULL)
                    """);
        }
    }

    private static void convertOldNumbers(Connection connection) throws SQLException {
        try (Statement select = connection.createStatement();
             ResultSet rows = select.executeQuery("SELECT login, phone_number, country_code FROM users");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE users SET phone = ?, phone_legacy = ? WHERE login = ?")) {
            while (rows.next()) {
                String typed = rows.getString("phone_number").strip();
                String e164 = toE164(typed, rows.getString("country_code"));
                update.setString(1, e164);
                update.setString(2, e164 == null ? typed : null);
                update.setString(3, rows.getString("login"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    /**
     * Reads an old number.
     * @param typed the number as it was stored
     * @param countryCode the ISO code of the profile's country, or null
     * @return the number in E.164, or null when it cannot be read
     */
    static String toE164(String typed, String countryCode) {
        try {
            PhoneNumber number = UTIL.parse(typed, countryCode == null ? "ZZ" : countryCode);
            return UTIL.isValidNumber(number) ? UTIL.format(number, PhoneNumberFormat.E164) : null;
        } catch (NumberParseException e) {
            return null;
        }
    }
}
