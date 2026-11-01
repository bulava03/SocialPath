package com.socialpath.repository;

import com.socialpath.entity.EmailLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailLinkRepository extends JpaRepository<EmailLink, String> {

    Optional<EmailLink> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("delete from EmailLink l where l.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
