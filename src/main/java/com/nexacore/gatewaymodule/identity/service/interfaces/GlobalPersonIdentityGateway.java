package com.nexacore.gatewaymodule.identity.service.interfaces;

import com.nexacore.gatewaymodule.identity.dto.GlobalPersonIdentityDto;
import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

public interface GlobalPersonIdentityGateway extends ModuleGateway {
    GlobalPersonIdentityDto synchronizeFromKyc(GlobalPersonIdentityDto person, Long actorId);

    boolean existsById(Long personId);
}
