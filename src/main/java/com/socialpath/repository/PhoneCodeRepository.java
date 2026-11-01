package com.socialpath.repository;

import com.socialpath.entity.PhoneCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PhoneCodeRepository extends JpaRepository<PhoneCode, String> {

    Optional<PhoneCode> findByUserLogin(String userLogin);

    @Modifying
    @Transactional
    @Query("delete from PhoneCode c where c.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
