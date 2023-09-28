package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.repository.GroupRepository;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.SearchService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public Object[] SearchUsersAndGroups(String searchValuesString, String thisLogin) {
        List<String> searchValues = Arrays.asList(searchValuesString.split(" "));

        List<User> usersList = searchValues.stream()
                .flatMap(str -> userRepository.findMatchingUsers(str).stream())
                .distinct()
                .toList();

        List<UserSearchResult> users = new ArrayList<>();

        for (User element : usersList
             ) {
            if (!element.getLogin().equals(thisLogin)) {
                UserSearchResult userSearchResult = modelMapper.map(element, UserSearchResult.class);
                userSearchResult.setAnotherUserLogin(element.getLogin());
                users.add(userSearchResult);
            }
        }

        List<GroupSearchResult> groups = groupRepository.findByNameIn(searchValues).stream()
                .map(group -> modelMapper.map(group, GroupSearchResult.class))
                .collect(Collectors.toList());

        return new Object[]{users, groups};
    }
}
