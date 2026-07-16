package com.socialpath.dto.response;

import lombok.Data;
/**
 * Flat view model of a user for rendering the registration and profile-edit
 * forms. The date of birth is split into day/month/year so the form can bind
 * three separate selects. Used both when first showing a form and when
 * re-showing it with validation errors.
 */
@Data
public class UserFormView {
    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private int day;
    private String month;
    private int year;
    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;
}
