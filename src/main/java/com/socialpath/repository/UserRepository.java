package com.socialpath.repository;

import com.socialpath.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for users. The fine-grained update methods of the
 * MongoDB edition (push/pull on embedded arrays) are gone: friendship and
 * invite mutations are now transactional load-modify-save operations in
 * UserServiceImpl, and group membership / publications are owned by their own
 * tables and queried through GroupRepository and CommentsRepository.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    default User findByLogin(String login) {
        return login == null ? null : findById(login).orElse(null);
    }

    List<User> findByLoginIn(List<String> logins);

    /**
     * Loads only what a feed shows about authors, for many logins at once.
     * @param logins the authors' logins
     * @return one view per existing login
     */
    @Query("""
            select u.login as login, u.firstName as firstName, u.lastName as lastName, u.imageId as imageId
            from User u where u.login in :logins
            """)
    List<AuthorView> findAuthors(@Param("logins") Collection<String> logins);

    List<User> findByImageIdIsNotNull();

    /**
     * The account a number signs in. Only a confirmed number counts, and a
     * unique key keeps it to one account.
     * @param phone the number in E.164
     * @return the account, or null when no account has confirmed the number
     */
    @Query("select u from User u where u.phone = :phone and u.phoneVerified = true")
    User findByVerifiedPhone(@Param("phone") String phone);

    /**
     * Whether another account has already confirmed a number.
     * @param phone the number in E.164
     * @param login the account to leave out, or null for a new one
     * @return true when the number belongs to someone else
     */
    @Query("""
            select count(u) > 0 from User u
            where u.phone = :phone and u.phoneVerified = true and (:login is null or u.login <> :login)
            """)
    boolean isPhoneVerifiedByOther(@Param("phone") String phone, @Param("login") String login);

    /**
     * The account an email address signs in. Only a confirmed address
     * counts, and a unique key keeps it to one account. The column's
     * collation makes the comparison ignore case.
     * @param email the address
     * @return the account, or null when no account has confirmed the address
     */
    @Query("select u from User u where u.email = :email and u.emailVerified = true")
    User findByVerifiedEmail(@Param("email") String email);

    /**
     * Whether another account has already confirmed an email address.
     * @param email the address
     * @param login the account to leave out, or null for a new one
     * @return true when the address belongs to someone else
     */
    @Query("""
            select count(u) > 0 from User u
            where u.email = :email and u.emailVerified = true and (:login is null or u.login <> :login)
            """)
    boolean isEmailVerifiedByOther(@Param("email") String email, @Param("login") String login);

    /**
     * Removes registrations nobody confirmed. Their phone codes and anything
     * else hanging off them go with them through ON DELETE CASCADE.
     * @param before registrations sent before this are removed
     * @return how many were removed
     */
    @Transactional
    @Modifying
    @Query("delete from User u where u.pendingSince < :before")
    int deletePendingBefore(@Param("before") LocalDateTime before);

    @Transactional
    @Modifying
    @Query("update User u set u.imageId = :imageId where u.login = :login")
    void updateImageId(@Param("login") String login, @Param("imageId") String imageId);

    /**
     * Case-insensitive substring search across the profile fields, the SQL
     * counterpart of the previous multi-field regex query. A single LIKE-based
     * query keeps it index-unfriendly but simple; a FULLTEXT index would be
     * the next step if profiles grow large. A phone number or email address
     * is searched only when it is confirmed and its owner shows it to
     * everyone: otherwise search would reveal who a hidden one belongs to.
     * Registrations still waiting for their phone code are not accounts yet
     * and are left out.
     * @param searchValue the substring to look for
     * @return users matching in any profile field
     */
    @Query("""
            select u from User u
            left join u.country c
            where u.pendingSince is null and (
                  lower(u.firstName) like lower(concat('%', :q, '%'))
               or lower(u.lastName) like lower(concat('%', :q, '%'))
               or (u.emailVisibility = com.socialpath.entity.ContactVisibility.EVERYONE and u.emailVerified = true
                   and lower(u.email) like lower(concat('%', :q, '%')))
               or (u.phoneVisibility = com.socialpath.entity.ContactVisibility.EVERYONE and u.phoneVerified = true
                   and u.phone like concat('%', :q, '%'))
               or lower(c.name) like lower(concat('%', :q, '%'))
               or lower(u.region) like lower(concat('%', :q, '%'))
               or lower(u.city) like lower(concat('%', :q, '%'))
               or lower(u.education) like lower(concat('%', :q, '%'))
               or lower(u.workplace) like lower(concat('%', :q, '%')))
            """)
    List<User> findMatchingUsers(@Param("q") String searchValue);
}
