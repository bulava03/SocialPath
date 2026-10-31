package com.socialpath.repository;

import com.socialpath.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    List<UserSession> findByUserLoginOrderByLastSeenAtDesc(String userLogin);

    /**
     * Ends one of the user's sessions; the login check keeps a user from
     * ending someone else's.
     * @param id the session
     * @param login the user it must belong to
     * @return how many rows were deleted (0 or 1)
     */
    @Modifying
    @Transactional
    @Query("delete from UserSession s where s.id = :id and s.userLogin = :login")
    int deleteByIdAndUserLogin(@Param("id") String id, @Param("login") String login);

    /**
     * Ends every session of the user except the one given.
     * @param login whose sessions
     * @param keepId the session to keep
     * @return how many rows were deleted
     */
    @Modifying
    @Transactional
    @Query("delete from UserSession s where s.userLogin = :login and s.id <> :keepId")
    int deleteOthers(@Param("login") String login, @Param("keepId") String keepId);

    /**
     * Deletes the sessions past their expiry.
     * @param now the current time
     * @return how many rows were deleted
     */
    @Modifying
    @Transactional
    @Query("delete from UserSession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
