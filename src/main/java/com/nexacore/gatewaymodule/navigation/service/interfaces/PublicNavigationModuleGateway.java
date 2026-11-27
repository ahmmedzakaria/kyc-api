package com.nexacore.gatewaymodule.navigation.service.interfaces;

import com.nexacore.gatewaymodule.navigation.dto.PublicNavigationContextDto;
import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;
import java.util.Set;

public interface PublicNavigationModuleGateway extends ModuleGateway {
    PublicNavigationContextDto getPublicNavigation(String clientCode, Set<String> surfaces);
}
