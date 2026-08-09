package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.commonmodule.dto.RoutePrivilegePolicyDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicy;
import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicyPrivilege;
import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicyPrivilegeId;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.layout.repository.LayoutRoutePolicyPrivilegeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutRoutePolicyRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutRoutePolicyService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LayoutRoutePolicyServiceImpl implements LayoutRoutePolicyService {
    private final LayoutRoutePolicyRepository policyRepository;
    private final LayoutRoutePolicyPrivilegeRepository policyPrivilegeRepository;
    private final PrivilegeRepository privilegeRepository;
    private final ClientApplicationRepository clientApplicationRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<RoutePrivilegePolicyDto> getEffectivePolicies(String clientCode) {
        Map<String, SysLayoutRoutePolicy> effective = new LinkedHashMap<>();
        policyRepository.findEffectiveCandidates(clientCode == null ? "" : clientCode)
                .forEach(policy -> effective.merge(policy.getRouteUrl(), policy,
                        (existing, candidate) -> candidate.getClientApplicationId() == null ? existing : candidate));

        return effective.values().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void synchronizePolicy(String clientCode,
                                  String routeUrl,
                                  PrivilegeMatchMode matchMode,
                                  Collection<String> privilegeCodes,
                                  Long actorId) {
        Long clientId = resolveClientId(clientCode);
        SysLayoutRoutePolicy policy = policyRepository.findByClientApplicationIdAndRouteUrl(clientId, normalizeRoute(routeUrl))
                .orElseGet(SysLayoutRoutePolicy::new);
        policy.setClientApplicationId(clientId);
        policy.setRouteUrl(normalizeRoute(routeUrl));
        policy.setMatchMode(matchMode);
        policy.setActive(true);
        if (policy.getCreatedBy() == null) {
            policy.setCreatedBy(actorId);
        }
        policy.setUpdatedBy(actorId);
        SysLayoutRoutePolicy saved = policyRepository.save(policy);

        policyPrivilegeRepository.deleteByRoutePolicyId(saved.getId());
        List<SysPrivPrivilege> privileges = privilegeRepository.findByPrivilegeCodeIn(privilegeCodes);
        if (privileges.size() != new LinkedHashSet<>(privilegeCodes).size()) {
            throw new IllegalStateException("Cannot synchronize route policy with missing privileges: " + routeUrl);
        }
        policyPrivilegeRepository.saveAll(privileges.stream().map(privilege -> {
            SysLayoutRoutePolicyPrivilege link = SysLayoutRoutePolicyPrivilege.builder()
                    .id(new SysLayoutRoutePolicyPrivilegeId(saved.getId(), privilege.getId()))
                    .routePolicy(saved)
                    .privilege(privilege)
                    .active(true)
                    .build();
            link.setCreatedBy(actorId);
            link.setUpdatedBy(actorId);
            return link;
        }).toList());
    }

    private RoutePrivilegePolicyDto toDto(SysLayoutRoutePolicy policy) {
        LinkedHashSet<String> codes = policyPrivilegeRepository.findByRoutePolicyIdAndActiveTrue(policy.getId()).stream()
                .filter(link -> link.getPrivilege().isActive())
                .map(link -> link.getPrivilege().getPrivilegeCode())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return RoutePrivilegePolicyDto.builder()
                .routeUrl(policy.getRouteUrl())
                .matchMode(policy.getMatchMode().name())
                .privilegeCodes(codes)
                .build();
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

    private String normalizeRoute(String routeUrl) {
        if (routeUrl == null || routeUrl.isBlank()) {
            throw new IllegalArgumentException("routeUrl is required");
        }
        String normalized = routeUrl.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}
