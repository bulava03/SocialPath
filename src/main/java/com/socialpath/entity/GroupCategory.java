package com.socialpath.entity;

/**
 * What a group is about, chosen by its owner from a fixed list. Stored by
 * name; the CHECK constraint in V8__group_details.sql lists the same names.
 */
public enum GroupCategory {
    /** Art, film, theatre, museums. */
    ARTS("Arts & culture"),
    /** Reading and writing. */
    BOOKS("Books"),
    /** Work, careers, entrepreneurship. */
    BUSINESS("Business"),
    /** Studying and teaching. */
    EDUCATION("Education"),
    /** Cooking and eating out. */
    FOOD("Food & cooking"),
    /** Video, board and tabletop games. */
    GAMES("Games"),
    /** Sport for health, wellbeing. */
    HEALTH("Health & fitness"),
    /** Listening and playing. */
    MUSIC("Music"),
    /** Research and popular science. */
    SCIENCE("Science"),
    /** Teams, fans, competitions. */
    SPORTS("Sports"),
    /** Software, gadgets, engineering. */
    TECHNOLOGY("Technology"),
    /** Trips and places. */
    TRAVEL("Travel"),
    /** Anything the list does not cover. */
    OTHER("Other");

    private final String label;

    GroupCategory(String label) {
        this.label = label;
    }

    /**
     * The name shown to people.
     * @return e.g. "Food & cooking"
     */
    public String getLabel() {
        return label;
    }
}
