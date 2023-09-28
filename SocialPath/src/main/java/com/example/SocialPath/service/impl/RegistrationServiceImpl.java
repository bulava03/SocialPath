package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.User;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.RegistrationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Validator validator;

    @Override
    public Object[] validateUser(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage).toList();
            return new Object[] { false, errorMessages.stream().findFirst() };
        } else if (userRepository.findByLogin(user.getLogin()) == null) {
            return new Object[] { false, "Такий логін вже зайнято." };
        } else {
            return new Object[] { true, "" };
        }
    }
    @Override
    public void addUser(User user) {
        userRepository.save(user);
    }
}
