package com.nexacore.systemmodule.clientaccess.service.implementations;

import com.nexacore.systemmodule.clientaccess.dto.ClientApplicationContextDto;
import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;
import com.nexacore.systemmodule.clientaccess.security.ClientApplicationContextHolder;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientAccessDecisionService;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientApplicationContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientApplicationContextServiceImpl implements ClientApplicationContextService {

    private final ClientAccessDecisionService clientAccessDecisionService;

    @Override
    public Optional<ClientApplicationContextDto> getCurrentClientContext() {
        return ClientApplicationContextHolder.get()
                .map(context -> context.clientApplication())
                .map(this::toDto);
    }

    @Override
    public Set<String> getCurrentClientPrivilegeCodes(Set<String> userPrivilegeCodes) {
        SysClientApplication application = ClientApplicationContextHolder.get()
                .map(context -> context.clientApplication())
                .orElse(null);
        return clientAccessDecisionService.filterPrivilegeCodesForClient(application, userPrivilegeCodes);
    }

    @Override
    public boolean hasCurrentClient() {
        return ClientApplicationContextHolder.get()
                .map(context -> context.clientApplication() != null)
                .orElse(false);
    }

    private ClientApplicationContextDto toDto(SysClientApplication application) {
        return ClientApplicationContextDto.builder()
                .clientApplicationId(application.getId())
                .clientCode(application.getClientCode())
                .clientType(application.getClientType())
                .build();
    }
}
