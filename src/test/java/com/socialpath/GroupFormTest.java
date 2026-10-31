package com.socialpath;

import com.socialpath.dto.request.GroupForm;
import com.socialpath.entity.Group;
import com.socialpath.entity.GroupCategory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static GroupForm named(String name) {
        GroupForm form = new GroupForm();
        form.setName(name);
        return form;
    }

    private boolean valid(GroupForm form) {
        return validator.validate(form).isEmpty();
    }

    @Test
    void nameOnly_IsEnough() {
        assertTrue(valid(named("Chess club")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankName_IsRefused(String name) {
        assertTrue(!valid(named(name)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://example.org/club?page=2", "HTTPS://Example.com/a-b"})
    void webLink_IsAccepted(String website) {
        GroupForm form = named("Club");
        form.setWebsite(website);

        assertTrue(valid(form), website);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:text/html,hi", "example.com", "ftp://example.com",
        "https://", "https://localhost", "https://exa mple.com"})
    void anythingButAWebLink_IsRefused(String website) {
        GroupForm form = named("Club");
        form.setWebsite(website);

        assertTrue(!valid(form), website);
    }

    @Test
    void tooLongDescription_IsRefused() {
        GroupForm form = named("Club");
        form.setDescription("x".repeat(1001));

        assertTrue(!valid(form));
        form.setDescription("x".repeat(1000));
        assertTrue(valid(form));
    }

    @Test
    void applyTo_TrimsAndStoresEmptyFieldsAsNone() {
        GroupForm form = named("  Chess club  ");
        form.setDescription("   ");
        form.setWebsite("");
        form.setCategory(GroupCategory.GAMES);
        Group group = new Group();

        form.applyTo(group);

        assertEquals("Chess club", group.getName());
        assertNull(group.getDescription());
        assertNull(group.getWebsite());
        assertEquals(GroupCategory.GAMES, group.getCategory());
    }

    @Test
    void from_FillsTheFormWithTheGroupsDetails() {
        Group group = new Group();
        group.setName("Chess club");
        group.setDescription("Weekly games.");
        group.setCategory(GroupCategory.GAMES);
        group.setWebsite("https://example.com");

        GroupForm form = GroupForm.from(group);

        assertEquals("Chess club", form.getName());
        assertEquals("Weekly games.", form.getDescription());
        assertEquals(GroupCategory.GAMES, form.getCategory());
        assertEquals("https://example.com", form.getWebsite());
    }

    @Test
    void other_NeedsItsOwnTopic() {
        GroupForm form = named("Club");
        form.setCategory(GroupCategory.OTHER);

        assertTrue(!valid(form));
        form.setCustomCategory("   ");
        assertTrue(!valid(form));
        form.setCustomCategory("Urban gardening");
        assertTrue(valid(form));
    }

    @Test
    void ownTopic_IsAtMostThirtyCharacters() {
        GroupForm form = named("Club");
        form.setCategory(GroupCategory.OTHER);
        form.setCustomCategory("x".repeat(31));

        assertTrue(!valid(form));
    }

    @Test
    void applyTo_KeepsTheOwnTopicOnlyWithOther() {
        GroupForm form = named("Club");
        form.setCategory(GroupCategory.OTHER);
        form.setCustomCategory("  Urban gardening ");
        Group group = new Group();

        form.applyTo(group);
        assertEquals("Urban gardening", group.getCustomCategory());

        // Typed while "Other" was chosen, then another topic picked: not kept.
        form.setCategory(GroupCategory.TRAVEL);
        form.applyTo(group);
        assertNull(group.getCustomCategory());
    }

    @Test
    void topic_IsTheOwnWordsForOtherAndTheListNameOtherwise() {
        Group group = new Group();
        assertNull(group.getTopic());

        group.setCategory(GroupCategory.FOOD);
        assertEquals("Food & cooking", group.getTopic());

        group.setCategory(GroupCategory.OTHER);
        group.setCustomCategory("Urban gardening");
        assertEquals("Urban gardening", group.getTopic());
    }
}
