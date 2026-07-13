package com.socialpath.repository.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.document.User;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public void save(User user) {
        mongoTemplate.insert(user);
    }

    @Override
    public User findByLogin(String login) {
        Query query = new Query(Criteria.where("_id").is(login));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public List<User> findAll() {
        return mongoTemplate.findAll(User.class);
    }

    @Override
    public void addPublicationToUser(ObjectId publicationId, String userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().push("publications", publicationId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void updateFieldsByLogin(String login, UserUpdate userUpdate) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update()
                .set("firstName", userUpdate.getFirstName())
                .set("lastName", userUpdate.getLastName())
                .set("email", userUpdate.getEmail())
                .set("phoneNumber", userUpdate.getPhoneNumber())
                .set("dateOfBirth", userUpdate.getDateOfBirth())
                .set("country", userUpdate.getCountry())
                .set("region", userUpdate.getRegion())
                .set("city", userUpdate.getCity())
                .set("education", userUpdate.getEducation())
                .set("workplace", userUpdate.getWorkplace())
                .set("imageId", userUpdate.getImageId());
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void updatePasswordByLogin(String login, String encodedPassword) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update()
                .set("password", encodedPassword);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void addGroupToUser(ObjectId groupId, String login) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update().push("groups", groupId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void removePublicationFromUser(String login, ObjectId id) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update().pull("publications", id);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public List<User> findMatchingUsers(String searchValue) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("firstName").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("lastName").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("email").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("phoneNumber").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("country").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("region").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("city").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("education").regex(Pattern.quote(searchValue), "i"),
                Criteria.where("workplace").regex(Pattern.quote(searchValue), "i")
        ));
        return mongoTemplate.find(query, User.class);
    }

    @Override
    public List<String> findUsersFriends(String login) {
        Query query = new Query(Criteria.where("_id").is(login));
        query.fields().include("friends");
        User user = mongoTemplate.findOne(query, User.class);
        return user != null ? user.getFriends() : null;
    }

    @Override
    public List<ObjectId> findUsersGroups(String login) {
        Query query = new Query(Criteria.where("_id").is(login));
        query.fields().include("groups");
        User user = mongoTemplate.findOne(query, User.class);
        return user != null ? user.getGroups() : null;
    }

    @Override
    public List<User> findByLoginIn(List<String> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        return mongoTemplate.find(query, User.class);
    }

    @Override
    public List<String> findUsersFriendsInvitations(String login) {
        Query query = new Query(Criteria.where("_id").is(login));
        query.fields().include("friendInvites");
        User user = mongoTemplate.findOne(query, User.class);
        return user != null ? user.getFriendInvites() : null;
    }

    @Override
    public void addToFriends(String myId, String friendId) {
        Query query = new Query(Criteria.where("_id").is(myId));
        Update update = new Update().push("friends", friendId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void inviteUser(String myId, String friendId) {
        Query query = new Query(Criteria.where("_id").is(friendId));
        Update update = new Update().push("friendInvites", myId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void removeFromInvitations(String myId, String friendId) {
        Query query = new Query(Criteria.where("_id").is(myId));
        Update update = new Update().pull("friendInvites", friendId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void removeFromFriends(String myId, String friendId) {
        Query query = new Query(Criteria.where("_id").is(myId));
        Update update = new Update().pull("friends", friendId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void removeFromGroups(String userId, ObjectId groupId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().pull("groups", groupId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public List<ObjectId> getPublicationsIdList(String login) {
        Query query = new Query(Criteria.where("_id").is(login));
        query.fields().include("publications");
        User user = mongoTemplate.findOne(query, User.class);
        return user != null ? user.getPublications() : null;
    }

    @Override
    public void addGroup(String login, ObjectId groupId) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update().push("groups", groupId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void removeGroup(String login, ObjectId groupId) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update().pull("groups", groupId);
        mongoTemplate.updateFirst(query, update, User.class);
    }

}
