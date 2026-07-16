package com.socialpath.repository;

import com.socialpath.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Case-insensitive substring search across the profile fields, the SQL
     * counterpart of the previous multi-field regex query. A single LIKE-based
     * query keeps it index-unfriendly but simple; a FULLTEXT index would be
     * the next step if profiles grow large.
     * @param searchValue the substring to look for
     * @return users matching in any profile field
     */
    @Query("""
            select u from User u
            where lower(u.firstName) like lower(concat('%', :q, '%'))
               or lower(u.lastName) like lower(concat('%', :q, '%'))
               or lower(u.email) like lower(concat('%', :q, '%'))
               or lower(u.phoneNumber) like lower(concat('%', :q, '%'))
               or lower(u.country) like lower(concat('%', :q, '%'))
               or lower(u.region) like lower(concat('%', :q, '%'))
               or lower(u.city) like lower(concat('%', :q, '%'))
               or lower(u.education) like lower(concat('%', :q, '%'))
               or lower(u.workplace) like lower(concat('%', :q, '%'))
            """)
    List<User> findMatchingUsers(@Param("q") String searchValue);
}
