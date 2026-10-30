package com.socialpath;

import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.SearchResults;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        groupRepository = mock(GroupRepository.class);
        service = new SearchServiceImpl(userRepository, groupRepository);
    }

    private static User user(String login) {
        User user = new User();
        user.setLogin(login);
        return user;
    }

    private static Group group(long id, String name) {
        Group group = new Group();
        group.setId(id);
        group.setName(name);
        return group;
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    void blankQuery_FindsNothingAndQueriesNothing(String query) {
        SearchResults results = service.searchUsersAndGroups(query, "me");

        assertTrue(results.users().isEmpty());
        assertTrue(results.groups().isEmpty());
        verify(userRepository, never()).findMatchingUsers(anyString());
        verify(groupRepository, never()).findByNameContaining(anyString());
    }

    @Test
    void eachWordIsSearched_AndSpacesCommasSemicolonsSplitWords() {
        service.searchUsersAndGroups("  anna,  ben;carl   dora ", "me");

        for (String word : List.of("anna", "ben", "carl", "dora")) {
            verify(userRepository).findMatchingUsers(word);
            verify(groupRepository).findByNameContaining(word);
        }
    }

    @Test
    void theSearcherIsNotAmongThePeopleFound() {
        when(userRepository.findMatchingUsers("anna")).thenReturn(List.of(user("me"), user("anna")));

        List<UserSearchResult> users = service.searchUsersAndGroups("anna", "me").users();

        assertEquals(List.of("anna"), users.stream().map(UserSearchResult::getLogin).toList());
    }

    @Test
    void someoneMatchingSeveralWords_IsListedOnce_InFirstMatchOrder() {
        when(userRepository.findMatchingUsers("anna")).thenReturn(List.of(user("anna"), user("ben")));
        when(userRepository.findMatchingUsers("riverside")).thenReturn(List.of(user("carl"), user("anna")));

        List<UserSearchResult> users = service.searchUsersAndGroups("anna riverside", "me").users();

        assertEquals(List.of("anna", "ben", "carl"), users.stream().map(UserSearchResult::getLogin).toList());
    }

    @Test
    void aGroupMatchingSeveralWords_IsListedOnce() {
        Group hiking = group(1L, "Hiking club");
        when(groupRepository.findByNameContaining("hiking")).thenReturn(List.of(hiking));
        when(groupRepository.findByNameContaining("club")).thenReturn(List.of(hiking, group(2L, "Book club")));

        List<GroupSearchResult> groups = service.searchUsersAndGroups("hiking club", "me").groups();

        assertEquals(List.of("Hiking club", "Book club"), groups.stream().map(GroupSearchResult::getName).toList());
    }
}
