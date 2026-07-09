package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.KycPerson;
import com.nexacore.kycmodule.person.entity.KycPersonDocument;
import com.nexacore.kycmodule.person.entity.PersonDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonDocumentRepository extends JpaRepository<KycPersonDocument, Long> {
    List<KycPersonDocument> findByPersonIdOrderByCreatedAtDesc(Long personId);
    Optional<KycPersonDocument> findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(Long personId, PersonDocumentType documentType);
    void deleteByPerson(KycPerson person);
}
