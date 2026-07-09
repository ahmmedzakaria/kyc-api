package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.KycPerson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<KycPerson, Long> {
    @Query("""
        SELECT p FROM KycPerson p
        WHERE
            LOWER(COALESCE(p.firstName, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(COALESCE(p.mobileNumber, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
        """)
    Page<KycPerson> searchByNameEmailOrMobile(String searchText, Pageable pageable);

    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<KycPerson> findByUsername(String username);
    Optional<KycPerson> findByEmail(String email);
    Optional<KycPerson> findByMobileNumber(String mobileNumber);
}
