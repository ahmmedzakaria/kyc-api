package com.nexacore.systemmodule.layout.api;

import com.nexacore.gatewaymodule.layout.service.interfaces.LayoutModuleGateway;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(transactionManager = "systemTransactionManager", readOnly = true)
public class SystemLayoutModuleGateway implements LayoutModuleGateway {

    private final LayoutContextService layoutContextService;

    @Override
    public Object getPublicLayout(String clientCode, String origin) {
        return layoutContextService.getPublicLayout(clientCode, origin);
    }
}
