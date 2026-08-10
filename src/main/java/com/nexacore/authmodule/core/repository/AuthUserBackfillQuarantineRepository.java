package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthUserBackfillQuarantine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserBackfillQuarantineRepository
        extends JpaRepository<AuthUserBackfillQuarantine, Long> {
    long countByResolvedFalse();
}
