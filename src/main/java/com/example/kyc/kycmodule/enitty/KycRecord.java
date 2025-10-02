package com.example.kyc.kycmodule.enitty;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "kyc_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    //@Column(unique = true, nullable = false)
    //private String nationalId;

    private String email;

    private String phone;

    // Photo will be stored externally (filesystem) - keep file reference
    @Column(name = "photo_path")
    private String photoPath;

    @Column(name = "photo_content_type")
    private String photoContentType;

    private Instant createdAt;
    private Instant updatedAt;
}
