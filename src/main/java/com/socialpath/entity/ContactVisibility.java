package com.socialpath.entity;

import java.util.Collection;

/**
 * Who sees a user's phone number or email address on their page. The owner
 * always sees them. Stored by name; V10 and V12 limit the columns to these
 * names.
 */
public enum ContactVisibility {
    /** Anyone who opens the page; the contact can also be found by search. */
    EVERYONE("Everyone", "Anyone who opens your page sees it, and can find you by it in search."),
    /** Only confirmed friends. */
    FRIENDS("Friends only", "Only your friends see it."),
    /** Nobody but the owner. */
    NOBODY("Only me", "Only you see it.");

    private final String label;
    private final String hint;

    ContactVisibility(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    /**
     * The name shown in the form and beside the contact on the owner's page.
     * @return e.g. "Friends only"
     */
    public String getLabel() {
        return label;
    }

    /**
     * Who sees the contact, said in full, for the profile form.
     * @return e.g. "Only your friends see it."
     */
    public String getHint() {
        return hint;
    }

    /**
     * Whether a viewer other than the owner may see the contact.
     * @param viewerLogin who is looking
     * @param ownersFriends the owner's friends
     * @return true when this setting lets them
     */
    public boolean allows(String viewerLogin, Collection<String> ownersFriends) {
        return this == EVERYONE || this == FRIENDS && ownersFriends.contains(viewerLogin);
    }
}
