package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysClientLayoutProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientLayoutProfileRepository extends JpaRepository<SysClientLayoutProfile, Long> {
    List<SysClientLayoutProfile> findByClientApplicationIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long clientApplicationId);
    Optional<SysClientLayoutProfile> findFirstByClientApplicationClientCodeAndDefaultProfileTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(String clientCode);
    boolean existsByClientApplicationIdAndDefaultProfileTrueAndActiveTrueAndIdNot(Long clientApplicationId, Long id);
}
