package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.authmodule.core.service.implementations.AuthApplicationContextService;
import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationContextService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivAction;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationCategory;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationCategoryRepository;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ActionRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubmoduleRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.UserPrivilegeRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutRoutePolicyService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutUiPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationContextDto;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import com.nexacore.systemmodule.accesscontrol.security.EffectiveTenantAccessContext;
import com.nexacore.systemmodule.accesscontrol.security.EffectiveTenantAccessContextHolder;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

class PrivilegeServiceImplTest {

    private final PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
    private final RolePrivilegeRepository rolePrivilegeRepository = mock(RolePrivilegeRepository.class);
    private final UserPrivilegeRepository userPrivilegeRepository = mock(UserPrivilegeRepository.class);
    private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    private final SubmoduleRepository submoduleRepository = mock(SubmoduleRepository.class);
    private final FeatureRepository featureRepository = mock(FeatureRepository.class);
    private final LayoutNavigationCategoryRepository navigationCategoryRepository = mock(LayoutNavigationCategoryRepository.class);
    private final ActionRepository actionRepository = mock(ActionRepository.class);
    private final AuthModuleGateway authModuleGateway = mock(AuthModuleGateway.class);
    private final ClientApplicationContextService clientApplicationContextService = mock(ClientApplicationContextService.class);
    private final AuthApplicationContextService authApplicationContextService = mock(AuthApplicationContextService.class);
    private final LayoutContextService layoutContextService = mock(LayoutContextService.class);
    private final LayoutRoutePolicyService layoutRoutePolicyService = mock(LayoutRoutePolicyService.class);
    private final LayoutUiPolicyService layoutUiPolicyService = mock(LayoutUiPolicyService.class);
    private final com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache authorizationDataCache = mock(com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache.class);
    private final PrivilegeServiceImpl service = new PrivilegeServiceImpl(
            privilegeRepository,
            rolePrivilegeRepository,
            userPrivilegeRepository,
            moduleRepository,
            submoduleRepository,
            featureRepository,
            navigationCategoryRepository,
            actionRepository,
            authModuleGateway,
            clientApplicationContextService,
            authApplicationContextService,
            layoutContextService,
            layoutRoutePolicyService,
            layoutUiPolicyService,
            authorizationDataCache
    );

    @Test
    void privilegeDefinitionsAreLoadedFromTheNormalizedDatabaseCatalog() {
        SysPrivModule module = SysPrivModule.builder().code("11").name("System").active(true).build();
        SysPrivSubmodule submodule = SysPrivSubmodule.builder().module(module).code("01").name("Access Control").active(true).build();
        SysLayoutNavigationCategory category = SysLayoutNavigationCategory.builder()
                .featureTypeCode("01").categoryName("Setup").active(false).build();
        SysPrivFeature feature = SysPrivFeature.builder().submodule(submodule).navigationCategory(category)
                .featureCode("001").featureName("Privilege Catalog").active(true).build();
        SysPrivAction action = SysPrivAction.builder().actionCode("80").actionName("Synchronize")
                .displayOrder(80).active(true).build();
        when(featureRepository.findActiveCatalogFeatures()).thenReturn(List.of(feature));
        when(actionRepository.findAllByActiveTrueOrderByDisplayOrderAscActionCodeAsc()).thenReturn(List.of(action));

        var definitions = service.getModulePrivilegeDefinitions();

        assertThat(definitions).singleElement().satisfies(definition -> {
            assertThat(definition.getModuleName()).isEqualTo("System");
            assertThat(definition.getSubmoduleName()).isEqualTo("Access Control");
            assertThat(definition.getFeatureTypeName()).isEqualTo("Setup");
            assertThat(definition.getFeatureName()).isEqualTo("Privilege Catalog");
            assertThat(definition.getActions()).singleElement().satisfies(resultAction -> {
                assertThat(resultAction.getActionName()).isEqualTo("Synchronize");
                assertThat(resultAction.getPrivilegeCode()).isEqualTo("11010100180");
            });
        });
    }

    @Test
    void deactivationRequiresTheCurrentPresentedDependencyImpact() {
        SysPrivPrivilege privilege=SysPrivPrivilege.builder().id(77L).privilegeCode("11010100180").active(true).build();
        when(privilegeRepository.findByPrivilegeCode("11010100180")).thenReturn(java.util.Optional.of(privilege));
        when(privilegeRepository.findById(77L)).thenReturn(java.util.Optional.of(privilege));
        when(privilegeRepository.countRoleDependencies(77L)).thenReturn(2L);
        when(privilegeRepository.countApiDependencies("11010100180")).thenReturn(1L);
        var impact=service.getImpact("11010100180");
        assertThat(impact.totalActiveDependencies()).isEqualTo(3);

        var result=service.deactivate(new com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeDeactivateRequest(
                "11010100180",impact.version(),true));

        assertThat(result.deactivated()).isTrue();
        assertThat(privilege.isActive()).isFalse();
        verify(privilegeRepository).save(privilege);
    }

    {
        when(authorizationDataCache.userPrivileges(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
                ((java.util.function.Supplier<Set<String>>) invocation.getArgument(2)).get());
    }

    @AfterEach
    void clearEffectiveTenant() {
        EffectiveTenantAccessContextHolder.clear();
    }

    @Test
    void applicationContextIncludesServerValidatedTenantScopesAndVersion() {
        when(authModuleGateway.getUserAccess("operator")).thenReturn(AuthUserAccessDto.builder()
                .userId(10L).roleIds(Set.of()).admin(false).build());
        when(privilegeRepository.findActivePrivilegeCodesByUserId(10L)).thenReturn(List.of("01010200101"));
        when(clientApplicationContextService.getCurrentClientPrivilegeCodes(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientApplicationContextService.getCurrentClientContext()).thenReturn(java.util.Optional.of(
                ClientApplicationContextDto.builder().clientApplicationId(3L).clientCode("WEB")
                        .clientType(ClientApplicationType.WEB).build()));
        when(authApplicationContextService.applyAuthPolicy(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        EffectiveTenantAccessContextHolder.set(new EffectiveTenantAccessContext(
                7L, "TENANT_7", "tenant.example.test", 10L, 3L,
                Set.of(new UserScopeAssignment(7L, 8L, 9L))));

        ApplicationContextDto context = service.getApplicationContext("operator");

        assertThat(context.getEffectiveTenant().tenantId()).isEqualTo(7L);
        assertThat(context.getEffectiveTenant().scopeAssignments())
                .singleElement().satisfies(scope -> {
                    assertThat(scope.businessId()).isEqualTo(8L);
                    assertThat(scope.branchId()).isEqualTo(9L);
                });
        assertThat(context.getAuthorizationVersion()).isNotBlank();
    }

}
