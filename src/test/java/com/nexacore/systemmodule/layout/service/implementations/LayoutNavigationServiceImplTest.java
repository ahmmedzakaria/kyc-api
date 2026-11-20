package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeature;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeatureGroup;
import com.nexacore.systemmodule.layout.entity.SysLayoutModuleGroup;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationCategory;
import com.nexacore.systemmodule.layout.repository.LayoutFeatureGroupRepository;
import com.nexacore.systemmodule.layout.repository.LayoutFeaturePrivilegeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutFeatureRepository;
import com.nexacore.systemmodule.layout.repository.LayoutModuleGroupRepository;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationCategoryRepository;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationModuleRepository;
import com.nexacore.systemmodule.layout.repository.LayoutRoutePolicyRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutCodeGenerationService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayoutNavigationServiceImplTest {

    @Mock private LayoutModuleGroupRepository moduleGroupRepository;
    @Mock private LayoutNavigationModuleRepository navigationModuleRepository;
    @Mock private LayoutNavigationCategoryRepository categoryRepository;
    @Mock private LayoutFeatureGroupRepository featureGroupRepository;
    @Mock private LayoutFeatureRepository featureRepository;
    @Mock private LayoutFeaturePrivilegeRepository featurePrivilegeRepository;
    @Mock private LayoutRoutePolicyRepository layoutRoutePolicyRepository;
    @Mock private ClientApplicationRepository clientApplicationRepository;
    @Mock private ClientFeaturePermissionRepository clientFeaturePermissionRepository;
    @Mock private PrivilegeRepository privilegeRepository;
    @Mock private LayoutCodeGenerationService codeGenerationService;
    @Mock private AuthModuleGateway authModuleGateway;

    @InjectMocks private LayoutNavigationServiceImpl service;

    @AfterEach
    void clearTenantContext() {
        ResolvedTenantContextHolder.clear();
    }

    @Test
    void excludesFeatureAndParentBranchWhenFeatureHasNoPrivilegeMapping() {
        ResolvedTenantContextHolder.set(new ResolvedTenantContext(7L, "TENANT_7", "tenant.example.test"));
        SysAccClientApplication client = SysAccClientApplication.builder()
                .id(10L)
                .clientCode("APP")
                .status(ClientApplicationStatus.ACTIVE)
                .build();
        SysLayoutModuleGroup moduleGroup = SysLayoutModuleGroup.builder()
                .id(20L).groupCode("GROUP").groupName("Group").active(true).displayOrder(1).build();
        SysPrivModule module = SysPrivModule.builder()
                .id(30L).moduleGroup(moduleGroup).navigationModuleCode("MODULE")
                .name("Module").navigationEnabled(true).active(true).displayOrder(1).build();
        SysLayoutNavigationCategory category = SysLayoutNavigationCategory.builder()
                .id(40L).navigationModule(module).categoryCode("CATEGORY")
                .categoryName("Category").active(true).displayOrder(1).build();
        SysLayoutFeatureGroup featureGroup = SysLayoutFeatureGroup.builder()
                .id(50L).navigationCategory(category).featureGroupCode("FEATURE_GROUP")
                .featureGroupName("Feature group").active(true).displayOrder(1).build();
        SysLayoutFeature feature = SysLayoutFeature.builder()
                .id(60L).version(0L).featureGroup(featureGroup).featureCode("FEATURE")
                .featureName("Feature").route("/feature").active(true).displayOrder(1).build();

        when(clientApplicationRepository.findByClientCode("APP")).thenReturn(Optional.of(client));
        when(clientFeaturePermissionRepository.findActivePrivilegeCodesByTenantIdAndClientApplicationId(7L, 10L)).thenReturn(Set.of());
        when(moduleGroupRepository.findByActiveTrueOrderByDisplayOrderAscGroupNameAsc()).thenReturn(List.of(moduleGroup));
        when(navigationModuleRepository.findByActiveTrueAndNavigationEnabledTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(module));
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAscCategoryNameAsc()).thenReturn(List.of(category));
        when(featureGroupRepository.findByActiveTrueOrderByDisplayOrderAscFeatureGroupNameAsc()).thenReturn(List.of(featureGroup));
        when(featureRepository.findByActiveTrueOrderByDisplayOrderAscFeatureNameAsc()).thenReturn(List.of(feature));
        when(featurePrivilegeRepository.findByLayoutFeatureIdAndActiveTrue(60L)).thenReturn(List.of());

        assertThat(service.getNavigationTree("APP", "user", Set.of())).isEmpty();
    }
}
