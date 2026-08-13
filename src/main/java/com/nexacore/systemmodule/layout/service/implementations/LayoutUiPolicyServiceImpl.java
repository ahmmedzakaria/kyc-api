package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.commonmodule.dto.UiPrivilegePolicyDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.layout.dto.LayoutUiPolicyRequestDto;
import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicy;
import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicyPrivilege;
import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicyPrivilegeId;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.layout.repository.LayoutUiPolicyPrivilegeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutUiPolicyRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutUiPolicyService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LayoutUiPolicyServiceImpl implements LayoutUiPolicyService {
    private final LayoutUiPolicyRepository policyRepository;
    private final LayoutUiPolicyPrivilegeRepository policyPrivilegeRepository;
    private final PrivilegeRepository privilegeRepository;
    private final ClientApplicationRepository clientApplicationRepository;
    private final AuthModuleGateway authModuleGateway;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<UiPrivilegePolicyDto> getEffectivePolicies(String clientCode) {
        Map<String, SysLayoutUiPolicy> effective = new LinkedHashMap<>();
        policyRepository.findEffectiveCandidates(clientCode == null ? "" : clientCode)
                .forEach(policy -> effective.merge(policy.getActionCode(), policy,
                        (existing, candidate) -> candidate.getClientApplicationId() == null ? existing : candidate));
        return effective.values().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public UiPrivilegePolicyDto save(LayoutUiPolicyRequestDto request, String actor) {
        Long actorId = authModuleGateway.getUserId(actor);
        synchronize(request.getClientCode(), request.getActionCode(),
                request.getMatchMode() == null ? PrivilegeMatchMode.ANY : request.getMatchMode(),
                request.getPrivilegeCodes(), request.getActive() == null || request.getActive(), actorId, request.getVersion(), true);
        Long clientId = resolveClientId(request.getClientCode());
        return toDto(policyRepository.findByClientApplicationIdAndActionCode(clientId, normalizeActionCode(request.getActionCode()))
                .orElseThrow());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void synchronizePolicy(String clientCode,
                                  String actionCode,
                                  PrivilegeMatchMode matchMode,
                                  Collection<String> privilegeCodes,
                                  Long actorId) {
        synchronize(clientCode, actionCode, matchMode, privilegeCodes, true, actorId, null, false);
    }

    private void synchronize(String clientCode,
                             String actionCode,
                             PrivilegeMatchMode matchMode,
                             Collection<String> privilegeCodes,
                             boolean active,
                             Long actorId,
                             Long expectedVersion,
                             boolean enforceVersion) {
        Long clientId = resolveClientId(clientCode);
        String normalizedActionCode = normalizeActionCode(actionCode);
        SysLayoutUiPolicy policy = policyRepository.findByClientApplicationIdAndActionCode(clientId, normalizedActionCode)
                .orElseGet(SysLayoutUiPolicy::new);
        if (enforceVersion && policy.getId() != null && !Objects.equals(policy.getVersion(), expectedVersion)) {
            throw new OptimisticLockingFailureException("UI policy changed since it was loaded");
        }
        policy.setClientApplicationId(clientId);
        policy.setActionCode(normalizedActionCode);
        policy.setMatchMode(matchMode);
        policy.setActive(active);
        if (policy.getCreatedBy() == null) {
            policy.setCreatedBy(actorId);
        }
        policy.setUpdatedBy(actorId);
        SysLayoutUiPolicy saved = policyRepository.save(policy);

        policyPrivilegeRepository.deleteByUiPolicyId(saved.getId());
        Set<String> distinctCodes = new LinkedHashSet<>(privilegeCodes == null ? List.of() : privilegeCodes);
        List<SysPrivPrivilege> privileges = privilegeRepository.findByPrivilegeCodeIn(distinctCodes);
        if (privileges.size() != distinctCodes.size()) {
            throw new IllegalStateException("Cannot synchronize UI policy with missing privileges: " + actionCode);
        }
        policyPrivilegeRepository.saveAll(privileges.stream().map(privilege -> {
            SysLayoutUiPolicyPrivilege link = SysLayoutUiPolicyPrivilege.builder()
                    .id(new SysLayoutUiPolicyPrivilegeId(saved.getId(), privilege.getId()))
                    .uiPolicy(saved)
                    .privilege(privilege)
                    .active(true)
                    .build();
            link.setCreatedBy(actorId);
            link.setUpdatedBy(actorId);
            return link;
        }).toList());
    }

    private UiPrivilegePolicyDto toDto(SysLayoutUiPolicy policy) {
        LinkedHashSet<String> codes = policyPrivilegeRepository.findByUiPolicyIdAndActiveTrue(policy.getId()).stream()
                .filter(link -> link.getPrivilege().isActive())
                .map(link -> link.getPrivilege().getPrivilegeCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return UiPrivilegePolicyDto.builder()
                .id(policy.getId())
                .version(policy.getVersion())
                .clientApplicationId(policy.getClientApplicationId())
                .clientCode(resolveClientCode(policy.getClientApplicationId()))
                .actionCode(policy.getActionCode())
                .matchMode(policy.getMatchMode().name())
                .active(policy.isActive())
                .privilegeCodes(codes)
                .build();
    }

    private String resolveClientCode(Long clientApplicationId) {
        return clientApplicationId == null ? null : clientApplicationRepository.findById(clientApplicationId)
                .map(SysAccClientApplication::getClientCode)
                .orElse(null);
    }

    private Long resolveClientId(String clientCode) {
        if (clientCode == null || clientCode.isBlank()) {
            return null;
        }
        SysAccClientApplication client = clientApplicationRepository.findByClientCode(clientCode)
                .orElseThrow(() -> new IllegalArgumentException("Client application not found: " + clientCode));
        if (client.getStatus() != ClientApplicationStatus.ACTIVE) {
            throw new IllegalArgumentException("Client application is not active: " + clientCode);
        }
        return client.getId();
    }

    private String normalizeActionCode(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            throw new IllegalArgumentException("actionCode is required");
        }
        return actionCode.trim().toLowerCase();
    }
}
