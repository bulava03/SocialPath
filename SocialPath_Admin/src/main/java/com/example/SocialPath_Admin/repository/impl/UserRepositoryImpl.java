package com.example.SocialPath_Admin.repository.impl;

import com.example.SocialPath_Admin.document.User;
import com.example.SocialPath_Admin.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public User findByLogin(String login) {
        Query query = new Query(Criteria.where("_id").is(login));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public User findByLoginAndPassword(String login, String password) {
        Query query = new Query(Criteria.where("_id").is(login).and("password").is(password));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public void removePublicationFromUser(String login, ObjectId id) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update().pull("publications", id);
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
    public void updateBan(String login, LocalDateTime date) {
        Query query = new Query(Criteria.where("_id").is(login));
        Update update = new Update().set("ban", date);
        mongoTemplate.updateFirst(query, update, User.class);
    }

    @Override
    public void removeGroupFromAllUsers(ObjectId group) {
        Query query = new Query(Criteria.where("groups").in(group));
        Update update = new Update().pull("groups", group);
        mongoTemplate.updateMulti(query, update, User.class);
    }

    @Override
    public void removePublicationIdsFromUser(List<ObjectId> publicationIds) {
        Query query = new Query(Criteria.where("publications").ne(null));
        Update update = new Update().pullAll("publications", publicationIds.toArray());
        mongoTemplate.updateMulti(query, update, User.class);
    }

    @Override
    public void removeUserFromUserArrays(String objectId) {
        Query query = new Query();
        Update update = new Update().pull("friends", objectId).pull("friendsRequest", objectId);
        mongoTemplate.updateMulti(query, update, User.class);
    }

}
