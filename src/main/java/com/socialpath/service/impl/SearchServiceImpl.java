package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.SearchResults;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.SearchService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Override
    public SearchResults searchUsersAndGroups(String searchValuesString, String thisLogin) {
        if (searchValuesString == null || searchValuesString.isBlank()) {
            return new SearchResults(List.of(), List.of());
        }

        List<String> searchValues = Arrays.stream(searchValuesString.split("[,;]\\s*|\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<User> usersList = searchValues.stream()
                .flatMap(str -> userRepository.findMatchingUsers(str).stream())
                .distinct()
                .toList();

        Map<String, User> uniqueUsersMap = new LinkedHashMap<>();
        usersList.forEach(user -> uniqueUsersMap.putIfAbsent(user.getLogin(), user));
        usersList = new ArrayList<>(uniqueUsersMap.values());

        List<UserSearchResult> users = usersList.stream()
                .filter(user -> !user.getLogin().equals(thisLogin))
                .map(UserSearchResult::from)
                .toList();

        Map<Long, Group> uniqueGroupsMap = new LinkedHashMap<>();
        searchValues.stream()
                .flatMap(str -> groupRepository.findByNameContaining(str).stream())
                .forEach(group -> uniqueGroupsMap.putIfAbsent(group.getId(), group));

        List<GroupSearchResult> groups = uniqueGroupsMap.values().stream()
                .map(GroupSearchResult::from)
                .toList();

        return new SearchResults(users, groups);
    }
}
