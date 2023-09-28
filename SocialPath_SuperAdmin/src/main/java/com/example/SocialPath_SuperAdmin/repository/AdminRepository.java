package com.example.SocialPath_SuperAdmin.repository;

import com.example.SocialPath_SuperAdmin.document.Admin;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends MongoRepository<Admin, String> {
}
