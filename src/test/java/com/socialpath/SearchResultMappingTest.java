package com.socialpath;

import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.entity.Country;
import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchResultMappingTest {

    @Test
    void userFrom_CopiesProfileAndCountryName() {
        Country country = new Country();
        country.setCode("UA");
        country.setName("Ukraine");
        User user = new User();
        user.setLogin("anna");
        user.setFirstName("Anna");
        user.setCity("Kyiv");
        user.setCountry(country);

        UserSearchResult result = UserSearchResult.from(user);

        assertEquals("anna", result.getLogin());
        assertEquals("anna", result.getAnotherUserLogin());
        assertEquals("Anna", result.getFirstName());
        assertEquals("Kyiv", result.getCity());
        assertEquals("Ukraine", result.getCountryName());
    }

    @Test
    void userFrom_WithoutCountry_LeavesCountryNameEmpty() {
        User user = new User();
        user.setLogin("anna");

        assertNull(UserSearchResult.from(user).getCountryName());
    }

    @Test
    void groupFrom_ConvertsIdToString() {
        Group group = new Group();
        group.setId(42L);
        group.setName("Hiking");
        group.setImageId("abc.jpg");

        GroupSearchResult result = GroupSearchResult.from(group);

        assertEquals("42", result.getId());
        assertEquals("Hiking", result.getName());
        assertEquals("abc.jpg", result.getImageId());
    }
}
