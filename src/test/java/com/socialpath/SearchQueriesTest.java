package com.socialpath;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** The LIKE-based search queries for people and groups. */
class SearchQueriesTest extends MySqlRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @BeforeEach
    void data() {
        jdbc.update("INSERT INTO countries (code, name) VALUES ('ZZ', 'Testland')");
        user("anna");
        user("ben");
        jdbc.update("UPDATE users SET first_name = 'Anna', city = 'Riverside', country_code = 'ZZ' WHERE login = 'anna'");
        // ben has no country: the country join must not drop him from the results.
        jdbc.update("UPDATE users SET first_name = 'Benedict', city = 'Lakeside' WHERE login = 'ben'");

        jdbc.update("INSERT INTO social_groups (name, owner_login) VALUES ('Hiking Club', 'anna')");
        jdbc.update("INSERT INTO social_groups (name, owner_login) VALUES ('Book club', 'ben')");
    }

    @Test
    void people_AreFoundByAnyProfileFieldOrCountry_IgnoringCase() {
        assertThat(userRepository.findMatchingUsers("ANNA")).extracting(User::getLogin).containsExactly("anna");
        assertThat(userRepository.findMatchingUsers("lakes")).extracting(User::getLogin).containsExactly("ben");
        assertThat(userRepository.findMatchingUsers("side"))
                .extracting(User::getLogin).containsExactlyInAnyOrder("anna", "ben");
        assertThat(userRepository.findMatchingUsers("testland")).extracting(User::getLogin).containsExactly("anna");
        // ben has no country: the country join must not drop him.
        assertThat(userRepository.findMatchingUsers("benedict")).extracting(User::getLogin).containsExactly("ben");
        assertThat(userRepository.findMatchingUsers("nobody-like-this")).isEmpty();
    }

    @Test
    void people_AreFoundByPhoneOnlyWhenTheyShowItToEveryone() {
        jdbc.update("""
                UPDATE users SET phone = '+12015550199', phone_verified = TRUE, phone_visibility = 'EVERYONE'
                WHERE login = 'anna'""");
        jdbc.update("""
                UPDATE users SET phone = '+12015550188', phone_verified = TRUE, phone_visibility = 'FRIENDS'
                WHERE login = 'ben'""");

        assertThat(userRepository.findMatchingUsers("2015550199")).extracting(User::getLogin).containsExactly("anna");
        // A hidden number must not tell whose it is.
        assertThat(userRepository.findMatchingUsers("2015550188")).isEmpty();
    }

    @Test
    void groups_AreFoundByAnyPartOfTheName_IgnoringCase() {
        assertThat(groupRepository.findByNameContaining("CLUB"))
                .extracting(Group::getName).containsExactlyInAnyOrder("Hiking Club", "Book club");
        assertThat(groupRepository.findByNameContaining("hik"))
                .extracting(Group::getName).containsExactly("Hiking Club");
        assertThat(groupRepository.findByNameContaining("chess")).isEmpty();
    }
}
