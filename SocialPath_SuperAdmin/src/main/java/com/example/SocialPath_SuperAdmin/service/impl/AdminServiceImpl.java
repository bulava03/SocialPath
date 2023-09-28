package com.example.SocialPath_SuperAdmin.service.impl;

import com.example.SocialPath_SuperAdmin.document.Admin;
import com.example.SocialPath_SuperAdmin.other.ValidationResult;
import com.example.SocialPath_SuperAdmin.repository.AdminRepository;
import com.example.SocialPath_SuperAdmin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private Validator validator;

    public ValidationResult validateAdmin(Admin admin) {
        Set<ConstraintViolation<Admin>> violations = validator.validate(admin);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());
            return ValidationResult.failure(errorMessages);
        } else if (adminRepository.findById(admin.getLogin()).isPresent()) {
            return ValidationResult.failure("Такий логін вже зайнято.");
        } else {
            return ValidationResult.success();
        }
    }

    public ValidationResult validateUpdateAdmin(Admin admin) {
        Set<ConstraintViolation<Admin>> violations = validator.validate(admin);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());
            return ValidationResult.failure(errorMessages);
        } else {
            return ValidationResult.success();
        }
    }

    public Admin findById(String id) {
        return adminRepository.findById(id).orElse(null);
    }

    public void addAdmin(Admin admin) {
        adminRepository.save(admin);
    }

    public void removeAdmin(Admin admin) {
        adminRepository.delete(admin);
    }

    public void changeAdmin(Admin admin) {
        adminRepository.save(admin);
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }
}
