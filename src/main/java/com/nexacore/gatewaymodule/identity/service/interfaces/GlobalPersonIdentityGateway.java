package com.nexacore.gatewaymodule.identity.service.interfaces;

import com.nexacore.gatewaymodule.identity.dto.GlobalPersonIdentityDto;
import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

import java.util.List;
import java.util.Optional;

public interface GlobalPersonIdentityGateway extends ModuleGateway {
    @Deprecated(forRemoval = true)
    GlobalPersonIdentityDto synchronizeFromKyc(GlobalPersonIdentityDto person, Long actorId);

    GlobalPersonIdentityDto create(GlobalPersonIdentityDto person, Long actorId);

    GlobalPersonIdentityDto update(Long personId, GlobalPersonIdentityDto person, Long actorId);

    Optional<GlobalPersonIdentityDto> findById(Long personId);

    Optional<GlobalPersonIdentityDto> findByEmail(String email);

    Optional<GlobalPersonIdentityDto> findByMobile(String mobile);

    List<Long> searchPersonIds(String query);

    boolean existsById(Long personId);
}
