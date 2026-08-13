package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findByPersonId(Long personId);
    Optional<AuthUser> findByExternalProviderAndExternalSubject(String externalProvider, String externalSubject);

    Optional<AuthUser> findByTenantIdAndNormalizedUsername(Long tenantId, String normalizedUsername);

    Optional<AuthUser> findByTenantIdAndPersonId(Long tenantId, Long personId);

    Optional<AuthUser> findByTenantIdAndExternalProviderAndExternalSubject(
            Long tenantId, String externalProvider, String externalSubject);

    Optional<AuthUser> findByIdAndTenantId(Long id, Long tenantId);

    List<AuthUser> findByTenantIdOrderByNormalizedUsernameAsc(Long tenantId);

    long countByTenantIdIsNull();
    long countByTenantId(Long tenantId);

    long countByNormalizedUsernameIsNull();

    boolean existsByPersonId(Long personId);

}
