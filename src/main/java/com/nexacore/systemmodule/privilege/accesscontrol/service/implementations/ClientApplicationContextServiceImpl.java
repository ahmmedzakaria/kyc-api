package com.nexacore.systemmodule.privilege.accesscontrol.service.implementations;

import com.nexacore.systemmodule.privilege.accesscontrol.dto.ClientApplicationContextDto;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.privilege.accesscontrol.security.ClientApplicationContextHolder;
import com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces.ClientAccessDecisionService;
import com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces.ClientApplicationContextService;
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
        SysPrivClientApplication application = ClientApplicationContextHolder.get()
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

    private ClientApplicationContextDto toDto(SysPrivClientApplication application) {
        return ClientApplicationContextDto.builder()
                .clientApplicationId(application.getId())
                .clientCode(application.getClientCode())
                .clientType(application.getClientType())
                .build();
    }
}
