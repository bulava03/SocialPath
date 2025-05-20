package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Grade;
import com.example.SocialPath.repository.GradeRepository;
import com.example.SocialPath.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private GradeRepository gradeRepository;

    public Grade findByUserLoginAndBizLogin(String userLogin, String bizLogin) {
        return gradeRepository.findByUserLoginAndBizLogin(userLogin, bizLogin);
    }

    public List<Grade> findAllByBizLogin(String bizLogin) {
        return gradeRepository.findAllByBizLogin(bizLogin);
    }

    public void save(Grade grade) {
        gradeRepository.save(grade);
    }

    public void deleteByUserLoginAndBizLogin(String userLogin, String bizLogin) {
        gradeRepository.deleteByUserLoginAndBizLogin(userLogin, bizLogin);
    }

    public double getAverageGrade(String bizLogin) {
        List<Grade> grades = findAllByBizLogin(bizLogin);

        if (grades.isEmpty()) {
            return 0;
        }

        double averageGrade = 0.0;

        for (Grade grade : grades) {
            averageGrade = averageGrade + grade.getRating();
        }

        return averageGrade / grades.size();
    }

    public int getUserGrade(String userLogin, String bizLogin) {
        return findByUserLoginAndBizLogin(userLogin, bizLogin).getRating();
    }

    public List<String> getReviewsByBizLogin(String bizLogin) {
        List<Grade> grades = findAllByBizLogin(bizLogin);

        if (grades.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> reviews = new ArrayList<>();

        for (Grade grade : grades) {
            if (!grade.getComment().isEmpty()) {
                reviews.add(grade.getComment());
            }
        }

        return reviews;
    }

}
