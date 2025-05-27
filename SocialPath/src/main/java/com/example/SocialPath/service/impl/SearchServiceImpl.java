package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.repository.GroupRepository;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.FileStorageService;
import com.example.SocialPath.service.GeoSearchService;
import com.example.SocialPath.service.SearchService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

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

    @Override
    public Object[] searchUsersAndGroupsAndBizes(String searchValuesString, String thisLogin) throws IOException {
        List<String> searchValues = Arrays.asList(searchValuesString.split(" "));

        List<User> usersList = searchValues.stream()
                .flatMap(str -> userRepository.findMatchingUsers(str).stream())
                .distinct()
                .toList();

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

                String file;
                if (element.getImageId() == null || element.getImageId().isEmpty()) {
                    file = null;
                } else {
                    GridFsResource resource = fileStorageService.getFileById(element.getImageId());
                    file = fileStorageService.convertGridFsFileToBase64(resource);
                }
                userSearchResult.setFile(file);

                bizes.add(userSearchResult);
            }
        }

        User myUser = userRepository.findByLogin(thisLogin);

        bizes = geoSearchService.findNearest(myUser.getLatitude(), myUser.getLongitude(), bizes);

        return new Object[]{users, groups, bizes};
    }
}
