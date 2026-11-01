package com.socialpath;

import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.entity.Country;
import com.socialpath.entity.Group;
import com.socialpath.entity.GroupCategory;
import com.socialpath.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchResultMappingTest {

    @Test
    void userFrom_CopiesProfileAndCountryName() {
        Country country = new Country();
        country.setCode("ZZ");
        country.setName("Testland");
        User user = new User();
        user.setLogin("anna");
        user.setFirstName("Anna");
        user.setCity("Riverside");
        user.setCountry(country);

        UserSearchResult result = UserSearchResult.from(user);

        assertEquals("anna", result.getLogin());
        assertEquals("anna", result.getAnotherUserLogin());
        assertEquals("Anna", result.getFirstName());
        assertEquals("Riverside", result.getCity());
        assertEquals("Testland", result.getCountryName());
    }

    @Test
    void userFrom_WithoutCountry_LeavesCountryNameEmpty() {
        User user = new User();
        user.setLogin("anna");

        assertNull(UserSearchResult.from(user).getCountryName());
    }

    @Test
    void groupFrom_CopiesTheCardDetails() {
        Group group = new Group();
        group.setId(42L);
        group.setName("Hiking");
        group.setImageId("abc.jpg");
        group.setCategory(GroupCategory.TRAVEL);
        group.setMembers(List.of("anna", "ben", "cara"));

        GroupSearchResult result = GroupSearchResult.from(group);

        assertEquals("42", result.getId());
        assertEquals("Hiking", result.getName());
        assertEquals("abc.jpg", result.getImageId());
        // The topic as Group#getTopic shows it (its cases are in GroupFormTest).
        assertEquals("Travel", result.getCategory());
        assertEquals(3, result.getMemberCount());
    }
}
