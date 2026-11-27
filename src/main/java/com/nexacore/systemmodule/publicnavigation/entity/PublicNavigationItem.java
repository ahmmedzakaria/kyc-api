package com.nexacore.systemmodule.publicnavigation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="sys_public_navigation_items")
@Getter @Setter @NoArgsConstructor
public class PublicNavigationItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long tenantId;
    @Column(nullable=false) private Long navigationGroupId;
    private Long parentId;
    @Column(nullable=false,length=80) private String itemCode;
    @Column(nullable=false,length=30) private String itemType;
    @Column(nullable=false,length=200) private String label;
    @Column(length=500) private String description;
    @Column(length=200) private String ariaLabel;
    @Column(length=500) private String routePath;
    @Column(length=1000) private String externalUrl;
    @Column(nullable=false,length=20) private String target="SELF";
    @Column(length=100) private String icon;
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
