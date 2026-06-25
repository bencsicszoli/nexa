package com.nexa.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Felhasználók keresése név vagy e-mail töredékre (kis/nagybetűtől függetlenül),
     * a hívó saját magát kizárva, név szerint rendezve. Üres szűrőre minden (más)
     * felhasználót ad — az „Emberek" böngészéséhez (#7). A teljes szövegkereső a #16.
     */
    @Query("""
            select u from User u
            where u.id <> :excludeId
              and (lower(u.displayName) like lower(concat('%', :q, '%'))
                or lower(u.email) like lower(concat('%', :q, '%')))
            order by u.displayName asc
            """)
    List<User> search(@Param("q") String q, @Param("excludeId") UUID excludeId, Pageable pageable);
}
