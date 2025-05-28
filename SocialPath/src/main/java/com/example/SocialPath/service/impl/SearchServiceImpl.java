package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.repository.GroupRepository;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.FileStorageService;
import com.example.SocialPath.service.GeoSearchService;
import com.example.SocialPath.service.ReverseGeolocationService;
import com.example.SocialPath.service.SearchService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GeoSearchService geoSearchService;
    @Autowired
    private ReverseGeolocationService reverseGeolocationService;

    @Override
    public Object[] searchUsersAndGroupsAndBizes(String searchValuesString, String thisLogin) throws IOException {
        List<String> searchValues = Arrays.stream(searchValuesString.split("[,;]\\s*|\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<User> usersList = searchValues.stream()
                .flatMap(str -> userRepository.findMatchingUsers(str).stream())
                .distinct()
                .toList();

        List<User> geoMatchedUsers = userRepository.findAll().stream()
                .filter(user -> user.getType() == 1)
                .filter(user -> reverseGeolocationService.countMatches(searchValues, user.getConcreteAddress()) > 0)
                .toList();

        Map<String, User> uniqueUsersMap = new LinkedHashMap<>();
        Stream.concat(usersList.stream(), geoMatchedUsers.stream())
                .forEach(user -> uniqueUsersMap.putIfAbsent(user.getLogin(), user));

        usersList = new ArrayList<>(uniqueUsersMap.values());

        List<UserSearchResult> users = new ArrayList<>();

        for (User element : usersList
             ) {
            if (!element.getLogin().equals(thisLogin) && element.getType() == 0) {
                UserSearchResult userSearchResult = modelMapper.map(element, UserSearchResult.class);
                userSearchResult.setAnotherUserLogin(element.getLogin());

                String file;
                if (element.getImageId() == null || element.getImageId().isEmpty()) {
                    file = null;
                } else {
                    GridFsResource resource = fileStorageService.getFileById(element.getImageId());
                    file = fileStorageService.convertGridFsFileToBase64(resource);
                }
                userSearchResult.setFile(file);

                users.add(userSearchResult);
            }
        }

        List<GroupSearchResult> groups = new ArrayList<>();

        List<Group> groupsList = groupRepository.findByNameIn(searchValues);
        for (Group group: groupsList) {
            GroupSearchResult groupSearchResult = modelMapper.map(group, GroupSearchResult.class);
            groupSearchResult.setId(group.getId().toString());

            String file;
            if (group.getImageId() == null || group.getImageId().isEmpty()) {
                file = null;
            } else {
                GridFsResource resource = fileStorageService.getFileById(group.getImageId());
                file = fileStorageService.convertGridFsFileToBase64(resource);
            }
            groupSearchResult.setFile(file);

            groups.add(groupSearchResult);
        }

        List<UserSearchResult> bizes = new ArrayList<>();

        for (User element : usersList
        ) {
            if (!element.getLogin().equals(thisLogin) && element.getType() == 1) {
                UserSearchResult userSearchResult = modelMapper.map(element, UserSearchResult.class);
                userSearchResult.setAnotherUserLogin(element.getLogin());

                int matches = reverseGeolocationService.countMatches(searchValues, userSearchResult.getConcreteAddress());

                String name = element.getName() != null ? element.getName().toLowerCase() : "";
                String slogan = element.getSlogan() != null ? element.getSlogan().toLowerCase() : "";
                String email = element.getEmail() != null ? element.getEmail().toLowerCase() : "";

                for (String word : searchValues) {
                    String lowerWord = word.toLowerCase();

                    if (name.contains(lowerWord)) matches++;
                    if (slogan.contains(lowerWord)) matches++;
                    if (email.contains(lowerWord)) matches++;
                }

                userSearchResult.setMatches(matches);

                String file;
                if (element.getImageId() == null || element.getImageId().isEmpty()) {
                    file = null;
                } else {
                    GridFsResource resource = fileStorageService.getFileById(element.getImageId());
                    file = fileStorageService.convertGridFsFileToBase64(resource);
                }
                userSearchResult.setFile(file);

                StringBuilder jobsString = new StringBuilder();

                if (element.getJobs() != null) {
                    for (String job : element.getJobs()) {
                        jobsString.append(job).append("; ");
                    }
                }

                String result;
                if (jobsString.isEmpty()) {
                    result = "";
                } else {
                    result = jobsString.substring(0, jobsString.length() - 2);
                }

                userSearchResult.setJobs(result);

                bizes.add(userSearchResult);
            }
        }

        User myUser = userRepository.findByLogin(thisLogin);

        bizes = geoSearchService.findNearest(myUser.getLatitude(), myUser.getLongitude(), bizes);

        return new Object[]{users, groups, bizes};
    }
}
