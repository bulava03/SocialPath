package com.example.SocialPath.service.impl;

import com.example.SocialPath.extraClasses.BizCreationForm;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.BizService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class BizServiceImpl implements BizService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Validator validator;

    @Override
    public Object[] validateBiz(BizCreationForm biz) {
        Set<ConstraintViolation<BizCreationForm>> violations = validator.validate(biz);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage).toList();
            return new Object[] { false, errorMessages.stream().findFirst() };
        } else {
            return new Object[] { true, "" };
        }
    }

}
