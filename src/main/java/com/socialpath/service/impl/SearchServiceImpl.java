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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ModelMapper modelMapper;

    @Override
    public SearchResults searchUsersAndGroups(String searchValuesString, String thisLogin) throws IOException {
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

        List<UserSearchResult> users = new ArrayList<>();

        for (User element : usersList) {
            if (!element.getLogin().equals(thisLogin)) {
                UserSearchResult userSearchResult = modelMapper.map(element, UserSearchResult.class);
                userSearchResult.setAnotherUserLogin(element.getLogin());


                users.add(userSearchResult);
            }
        }

        List<GroupSearchResult> groups = new ArrayList<>();

        List<Group> groupsList = groupRepository.findByNameIn(searchValues);
        for (Group group : groupsList) {
            GroupSearchResult groupSearchResult = modelMapper.map(group, GroupSearchResult.class);
            groupSearchResult.setId(group.getId().toString());


            groups.add(groupSearchResult);
        }

        return new SearchResults(users, groups);
    }
}
