package com.nexacore.gatewaymodule.layout.service.interfaces;

import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

public interface LayoutModuleGateway extends ModuleGateway {
    Object getPublicLayout(String clientCode, String origin);
}
