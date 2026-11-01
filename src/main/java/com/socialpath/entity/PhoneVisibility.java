package com.socialpath.entity;

/**
 * Who sees a user's phone number on their page. The owner always sees it.
 * Stored by name; V10 limits the column to these names.
 */
public enum PhoneVisibility {
    /** Anyone who opens the page; the number can also be found by search. */
    EVERYONE("Everyone"),
    /** Only confirmed friends. */
    FRIENDS("Friends only"),
    /** Nobody but the owner. */
    NOBODY("Only me");

    private final String label;

    PhoneVisibility(String label) {
        this.label = label;
    }

    /**
     * The name shown in the form.
     * @return e.g. "Friends only"
     */
    public String getLabel() {
        return label;
    }
}
