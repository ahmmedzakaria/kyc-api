package com.nexacore.systemmodule.publicnavigation.service;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.gatewaymodule.navigation.enums.PublicNavigationSurface;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.publicnavigation.entity.ClientPublicNavigationItem;
import com.nexacore.systemmodule.publicnavigation.entity.PublicNavigationGroup;
import com.nexacore.systemmodule.publicnavigation.entity.PublicNavigationItem;
import com.nexacore.systemmodule.publicnavigation.repository.ClientPublicNavigationItemRepository;
import com.nexacore.systemmodule.publicnavigation.repository.PublicNavigationGroupRepository;
import com.nexacore.systemmodule.publicnavigation.repository.PublicNavigationItemRepository;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicNavigationServiceTest {
    @Mock PublicNavigationGroupRepository groups;
    @Mock PublicNavigationItemRepository items;
    @Mock ClientPublicNavigationItemRepository assignments;
    @Mock ClientApplicationRepository clients;
    @Mock ClientApplicationTenantRepository clientTenants;
    @Mock DataScopeService dataScopes;
    @Mock AuthModuleGateway auth;
    PublicNavigationService service;

    @BeforeEach
    void setUp() {
        service = new PublicNavigationService(groups, items, assignments, clients, clientTenants, dataScopes, auth);
        ResolvedTenantContextHolder.set(new ResolvedTenantContext(7L, "DEMO", "demo.test"));
    }

    @AfterEach void clear() { ResolvedTenantContextHolder.clear(); }

    @Test
    void resolvesOnlyAssignedLinksAndTheirAncestorsInDeterministicShape() {
        SysAccClientApplication client = client();
        PublicNavigationGroup group = group();
        PublicNavigationItem parent = item(10L, null, "SHOP", "GROUP", null);
        PublicNavigationItem assigned = item(11L, 10L, "PRODUCTS", "INTERNAL_ROUTE", "/products");
        PublicNavigationItem hidden = item(12L, 10L, "CONTACT", "INTERNAL_ROUTE", "/contact");
        ClientPublicNavigationItem link = new ClientPublicNavigationItem();
        link.setPublicNavigationItemId(11L);

        when(clients.findByClientCode("CATALOG")).thenReturn(Optional.of(client));
        when(clientTenants.existsByClientApplicationIdAndTenantIdAndActiveTrue(3L, 7L)).thenReturn(true);
        when(groups.findByTenantIdAndSurfaceInAndActiveTrueOrderByDisplayOrderAscIdAsc(7L, Set.of("PRIMARY")))
                .thenReturn(List.of(group));
        when(items.findByTenantIdAndNavigationGroupIdInAndActiveTrueOrderByDisplayOrderAscIdAsc(7L, List.of(5L)))
                .thenReturn(List.of(parent, assigned, hidden));
        when(assignments.findByTenantIdAndClientApplicationIdAndActiveTrue(7L, 3L)).thenReturn(List.of(link));

        var result = service.getPublicNavigation("catalog", Set.of(PublicNavigationSurface.PRIMARY));

        assertThat(result.clientCode()).isEqualTo("CATALOG");
        assertThat(result.regions()).hasSize(1);
        assertThat(result.regions().getFirst().groups()).hasSize(1);
        var root = result.regions().getFirst().groups().getFirst().items().getFirst();
        assertThat(root.code()).isEqualTo("SHOP");
        assertThat(root.children()).extracting(value -> value.code()).containsExactly("PRODUCTS");
    }

    @Test
    void failsClosedWhenClientHasNoActiveTenantMapping() {
        when(clients.findByClientCode("CATALOG")).thenReturn(Optional.of(client()));
        when(clientTenants.existsByClientApplicationIdAndTenantIdAndActiveTrue(3L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.getPublicNavigation("CATALOG", Set.of(PublicNavigationSurface.PRIMARY)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No active client mapping");
    }

    private SysAccClientApplication client() {
        SysAccClientApplication value = new SysAccClientApplication();
        value.setId(3L); value.setClientCode("CATALOG"); value.setStatus(ClientApplicationStatus.ACTIVE);
        return value;
    }
    private PublicNavigationGroup group() {
        PublicNavigationGroup value = new PublicNavigationGroup();
        value.setId(5L); value.setTenantId(7L); value.setGroupCode("MAIN"); value.setGroupName("Main");
        value.setSurface("PRIMARY"); value.setActive(true); value.setDisplayOrder(1);
        return value;
    }
    private PublicNavigationItem item(long id, Long parentId, String code, String type, String route) {
        PublicNavigationItem value = new PublicNavigationItem();
        value.setId(id); value.setTenantId(7L); value.setNavigationGroupId(5L); value.setParentId(parentId);
        value.setItemCode(code); value.setItemType(type); value.setLabel(code); value.setRoutePath(route);
        value.setTarget("SELF"); value.setDisplayOrder((int) id); value.setActive(true);
        return value;
    }
}
