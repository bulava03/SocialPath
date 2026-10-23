package com.socialpath.service;

import com.socialpath.entity.Country;

import java.util.List;

/**
 * Access to the country reference list.
 */
public interface CountryService {

    /**
     * Returns all countries ordered by name, for the country select.
     * @return the reference list
     */
    List<Country> findAll();

    /**
     * Checks a submitted country code. An empty code is valid: the country
     * is optional.
     * @param code the submitted code, possibly null or blank
     * @return true when the code is empty or present in the reference list
     */
    boolean isValidCode(String code);

    /**
     * Resolves a submitted code to the country to store on a user.
     * @param code the submitted code, possibly null or blank
     * @return a reference to the country, or null when the code is empty
     */
    Country resolve(String code);
}
