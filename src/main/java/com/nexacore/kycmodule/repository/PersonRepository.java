package com.nexacore.kycmodule.repository;

import com.nexacore.kycmodule.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonRepository extends JpaRepository<Person, Long> {
    @Query("""
        SELECT p FROM Person p
        WHERE
            LOWER(COALESCE(p.firstName, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(COALESCE(p.mobileNumber, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
        """)
    Page<Person> searchByNameEmailOrMobile(String searchText, Pageable pageable);

    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
}
