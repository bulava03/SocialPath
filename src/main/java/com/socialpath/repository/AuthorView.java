package com.socialpath.repository;

/**
 * The few user fields a feed shows next to a post or comment. Loaded as a
 * projection so that rendering a feed does not pull whole users with their
 * friend lists.
 */
public interface AuthorView {
    String getLogin();

    String getFirstName();

    String getLastName();

    String getImageId();
}
