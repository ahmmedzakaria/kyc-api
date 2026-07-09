package com.nexacore.kycmodule.person.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_person_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycPersonDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private KycPerson person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PersonDocumentType documentType;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Column(length = 255)
    private String originalFilename;

    @Column(length = 255)
    private String contentType;

    private Long fileSize;

    private LocalDateTime createdAt = LocalDateTime.now();
}
