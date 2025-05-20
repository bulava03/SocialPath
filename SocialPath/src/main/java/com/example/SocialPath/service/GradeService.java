package com.example.SocialPath.service;

import com.example.SocialPath.document.Grade;

import java.util.List;

public interface GradeService {
    Grade findByUserLoginAndBizLogin(String userLogin, String bizLogin);
    List<Grade> findAllByBizLogin(String bizLogin);
    void save(Grade grade);
    void deleteByUserLoginAndBizLogin(String userLogin, String bizLogin);
    double getAverageGrade(String bizLogin);
    int getUserGrade(String userLogin, String bizLogin);
    List<String> getReviewsByBizLogin(String bizLogin);
}
