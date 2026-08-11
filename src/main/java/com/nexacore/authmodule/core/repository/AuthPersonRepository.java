package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AuthPersonRepository extends JpaRepository<AuthPerson, Long> {
    Optional<AuthPerson> findFirstByPrimaryEmailIgnoreCase(String email);
    Optional<AuthPerson> findFirstByPrimaryMobile(String mobile);

    @Query("""
            select person.id from AuthPerson person
            where person.active = true and (
                lower(coalesce(person.firstName, '')) like lower(concat('%', :query, '%')) or
                lower(coalesce(person.lastName, '')) like lower(concat('%', :query, '%')) or
                lower(coalesce(person.primaryEmail, '')) like lower(concat('%', :query, '%')) or
                lower(coalesce(person.primaryMobile, '')) like lower(concat('%', :query, '%'))
            )
            """)
    List<Long> searchActiveIds(@Param("query") String query);
}
