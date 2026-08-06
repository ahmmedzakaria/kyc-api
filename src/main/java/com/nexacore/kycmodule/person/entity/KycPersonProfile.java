package com.nexacore.kycmodule.person.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_person_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycPersonProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private KycPerson person;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "business_id")
    private Long businessId;
    @Column(name = "branch_id")
    private Long branchId;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
