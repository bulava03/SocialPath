package com.example.SocialPath.repository;

import com.example.SocialPath.document.Grade;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends MongoRepository<Grade, ObjectId> {
    Grade findByUserLoginAndBizLogin(String userLogin, String bizLogin);
    List<Grade> findAllByBizLogin(String bizLogin);
    void deleteByUserLoginAndBizLogin(String userLogin, String bizLogin);
}
