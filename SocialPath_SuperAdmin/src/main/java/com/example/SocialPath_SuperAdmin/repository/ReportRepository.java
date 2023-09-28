package com.example.SocialPath_SuperAdmin.repository;

import com.example.SocialPath_SuperAdmin.document.Report;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends MongoRepository<Report, ObjectId> {
    @Query("{'_id': ?0}")
    void updateStatusById(ObjectId id, String status);
}
