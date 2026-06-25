package com.nexa.group;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    /**
     * Csoportok keresése a névben szereplő töredékre (kis/nagybetűtől függetlenül),
     * név szerint rendezve. Üres szűrőre minden csoportot ad — a böngészéshez (#9).
     * A teljes szövegkereső a #16.
     */
    @Query("""
            select g from Group g
            where lower(g.name) like lower(concat('%', :q, '%'))
            order by g.name asc
            """)
    List<Group> search(@Param("q") String q, Pageable pageable);
}
