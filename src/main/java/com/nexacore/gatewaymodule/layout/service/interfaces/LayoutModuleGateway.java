package com.nexacore.gatewaymodule.layout.service.interfaces;

import com.nexacore.gatewaymodule.layout.dto.PublicLayoutContextDto;
import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

public interface LayoutModuleGateway extends ModuleGateway {
    PublicLayoutContextDto getPublicLayout(String clientCode, String origin);
}
