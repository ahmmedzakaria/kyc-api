package com.nexacore.systemmodule.publicnavigation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="sys_public_navigation_groups")
@Getter @Setter @NoArgsConstructor
public class PublicNavigationGroup {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long tenantId;
    @Column(nullable=false,length=80) private String groupCode;
    @Column(nullable=false,length=150) private String groupName;
    @Column(nullable=false,length=30) private String surface;
    @Column(nullable=false) private Integer displayOrder=100;
    @Column(nullable=false) private boolean active=true;
    @Version private Long version;
    @Column(nullable=false) private Long createdBy;
    @Column(nullable=false) private Long updatedBy;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    @PrePersist void create(){var now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
