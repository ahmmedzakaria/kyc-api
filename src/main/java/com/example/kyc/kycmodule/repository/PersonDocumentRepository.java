package com.example.kyc.kycmodule.repository;

import com.example.kyc.kycmodule.entity.Person;
import com.example.kyc.kycmodule.entity.PersonDocument;
import com.example.kyc.kycmodule.entity.PersonDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonDocumentRepository extends JpaRepository<PersonDocument, Long> {
    List<PersonDocument> findByPersonIdOrderByCreatedAtDesc(Long personId);
    Optional<PersonDocument> findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(Long personId, PersonDocumentType documentType);
    void deleteByPerson(Person person);
}
