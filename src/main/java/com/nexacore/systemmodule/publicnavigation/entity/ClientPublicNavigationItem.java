package com.nexacore.systemmodule.publicnavigation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="sys_acc_client_public_navigation_items")
@Getter @Setter @NoArgsConstructor
public class ClientPublicNavigationItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long tenantId;
    @Column(nullable=false) private Long clientApplicationId;
    @Column(nullable=false) private Long publicNavigationItemId;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false) private Long createdBy;
    @Column(nullable=false) private Long updatedBy;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    @PrePersist void create(){var now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
