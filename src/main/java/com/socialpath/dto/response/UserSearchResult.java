package com.socialpath.dto.response;

import com.socialpath.entity.User;
import lombok.Data;

/**
 * A user as shown in lists: search results, friends, friend requests and
 * group members.
 */
@Data
public class UserSearchResult {
    private String login;
    private String anotherUserLogin;
    private String imageId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String countryName;
    private String region;
    private String city;
    private String education;
    private String workplace;

    public static UserSearchResult from(User user) {
        UserSearchResult result = new UserSearchResult();
        result.setLogin(user.getLogin());
        result.setAnotherUserLogin(user.getLogin());
        result.setImageId(user.getImageId());
        result.setFirstName(user.getFirstName());
        result.setLastName(user.getLastName());
        result.setEmail(user.getEmail());
        result.setPhoneNumber(user.getPhoneNumber());
        result.setCountryName(user.getCountry() != null ? user.getCountry().getName() : null);
        result.setRegion(user.getRegion());
        result.setCity(user.getCity());
        result.setEducation(user.getEducation());
        result.setWorkplace(user.getWorkplace());
        return result;
    }
}
