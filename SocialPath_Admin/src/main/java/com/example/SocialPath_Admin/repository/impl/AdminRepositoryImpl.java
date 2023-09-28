package com.example.SocialPath_Admin.repository.impl;

import com.example.SocialPath_Admin.document.Admin;
import com.example.SocialPath_Admin.repository.AdminRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

@Repository
public class AdminRepositoryImpl implements AdminRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Admin findAdminByLoginAndPassword(String login, String password) {
        Query query = new Query(Criteria.where("login").is(login).and("password").is(password));
        return mongoTemplate.findOne(query, Admin.class);
    }

    @Override
    public void updatePassword(Admin admin) {
        Query query = new Query(Criteria.where("login").is(admin.getLogin()));
        Update update = new Update().set("password", admin.getPassword());
        mongoTemplate.updateFirst(query, update, Admin.class);
    }

    @Override
    public void addReport(Admin admin) {
        Query query = new Query(Criteria.where("login").is(admin.getLogin()));
        Update update = new Update().set("report", admin.getReport());
        mongoTemplate.updateFirst(query, update, Admin.class);
    }

    @Override
    public void setReportToNull(String adminId) {
        Query query = new Query(Criteria.where("_id").is(adminId));
        Update update = new Update().set("report", null);
        mongoTemplate.updateFirst(query, update, Admin.class);
    }

    @Override
    public void updateAdminReportsToNull(List<String> reportIds) {
        Query query = new Query(Criteria.where("report").in(reportIds));
        Update update = new Update().set("report", null);
        mongoTemplate.updateMulti(query, update, Admin.class);
    }

    @Override
    public void setReportsToNull(List<ObjectId> ids) {
        Query query = new Query(Criteria.where("report").in(ids));
        Update update = new Update().set("report", null);
        mongoTemplate.updateMulti(query, update, Admin.class);
    }

}
