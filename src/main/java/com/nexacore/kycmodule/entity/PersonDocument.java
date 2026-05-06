package com.nexacore.kycmodule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "person_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

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
