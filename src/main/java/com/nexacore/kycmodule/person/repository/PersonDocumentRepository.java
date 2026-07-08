package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.Person;
import com.nexacore.kycmodule.person.entity.PersonDocument;
import com.nexacore.kycmodule.person.entity.PersonDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonDocumentRepository extends JpaRepository<PersonDocument, Long> {
    List<PersonDocument> findByPersonIdOrderByCreatedAtDesc(Long personId);
    Optional<PersonDocument> findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(Long personId, PersonDocumentType documentType);
    void deleteByPerson(Person person);
}
