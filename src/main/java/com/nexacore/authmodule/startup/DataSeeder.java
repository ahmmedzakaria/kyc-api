package com.nexacore.authmodule.startup;

import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeMenuItemDto;
import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.entity.AuthRole;
import com.nexacore.authmodule.core.entity.AuthPerson;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.enums.AuthPersonStatus;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.AuthPersonRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncReportDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientPermissionService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubMenu;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.ModulePrivilegeProvider;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.SystemPrivilegeRegistryService;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.privilege.catalog.enums.PrivilegeAction;
import com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutRoutePolicyService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutUiPolicyService;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import com.nexacore.authmodule.core.service.UsernameNormalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.nexacore.authmodule.security.service.TenantAccountUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class DataSeeder {

    @Value("${nexacore.auth.bootstrap-tenant-id:0}")
    private long bootstrapTenantId;

    @Value("${nexacore.auth.bootstrap-system-password:}")
    private String bootstrapSystemPassword;

    private final UsernameNormalizer usernameNormalizer = new UsernameNormalizer();

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   AuthPersonRepository personRepository,
                                   SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                   List<ModulePrivilegeProvider> modulePrivilegeProviders,
                                   ClientApplicationService clientApplicationService,
                                   ClientPermissionService clientPermissionService,
                                   ClientApiRegistryService clientApiRegistryService,
                                   LayoutRoutePolicyService layoutRoutePolicyService,
                                   LayoutUiPolicyService layoutUiPolicyService,
                                   AuthClientAuthPolicyRepository authPolicyRepository,
                                   AuthenticationProperties authenticationProperties,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            requireBootstrapTenant();
            requireBootstrapPassword();
            AuthRole systemAdminRole = roleRepository.findByTenantIdIsNullAndRoleCodeIgnoreCase("ROLE_SYSTEM_ADMIN")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_SYSTEM_ADMIN"));
            systemPrivilegeRegistryService.syncApplicationCatalog();
            Set<String> adminPrivilegeCodes = seedModulePrivileges(systemPrivilegeRegistryService, modulePrivilegeProviders);
            Set<String> systemPrivilegeCodes = privilegesForModule(adminPrivilegeCodes, ApplicationModule.SYSTEM);
            assignRolePrivileges(systemPrivilegeRegistryService, systemAdminRole, systemPrivilegeCodes);
            AuthUser systemUser = seedSystemAdministrator(
                    userRepository, personRepository, passwordEncoder, systemAdminRole);

            runAsBootstrapAccount(systemUser.getId(), systemUser.getUsername(), () -> seedDefaultWebClient(
                    clientApplicationService,
                    clientPermissionService,
                    clientApiRegistryService,
                    adminPrivilegeCodes
            ));

            seedPersonRoutePolicies(layoutRoutePolicyService);
            seedPersonUiPolicies(layoutUiPolicyService);

            seedDefaultAuthPolicies(authPolicyRepository, authenticationProperties);
        };
    }

    private AuthUser seedSystemAdministrator(UserRepository userRepository,
                                             AuthPersonRepository personRepository,
                                             PasswordEncoder passwordEncoder,
                                             AuthRole systemAdminRole) {
        String username = "system_admin";
        AuthUser user = userRepository.findByTenantIdAndNormalizedUsername(
                bootstrapTenantId, usernameNormalizer.normalize(username)).orElse(null);
        if (user == null) {
            AuthPerson person = personRepository.save(AuthPerson.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .status(AuthPersonStatus.ACTIVE)
                    .active(true)
                    .createdBy(0L)
                    .updatedBy(0L)
                    .build());
            user = AuthUser.builder()
                    .tenantId(bootstrapTenantId)
                    .personId(person.getId())
                    .username(username)
                    .normalizedUsername(usernameNormalizer.normalize(username))
                    .password(passwordEncoder.encode(bootstrapSystemPassword))
                    .enabled(true)
                    .locked(false)
                    .roles(new HashSet<>(Set.of(systemAdminRole)))
                    .createdBy(0L)
                    .updatedBy(0L)
                    .build();
        } else {
            user.setRoles(new HashSet<>(Set.of(systemAdminRole)));
            user.setEnabled(true);
            user.setLocked(false);
        }
        user = userRepository.save(user);
        if (user.getScopeAssignments().isEmpty()) {
            var scope = com.nexacore.authmodule.core.entity.AuthUserScopeAssignment.builder()
                    .user(user)
                    .userId(user.getId())
                    .tenantId(bootstrapTenantId)
                    .active(true)
                    .createdBy(0L)
                    .updatedBy(0L)
                    .build();
            user.getScopeAssignments().add(scope);
            user = userRepository.save(user);
        }
        return user;
    }

    private void requireBootstrapTenant() {
        if (bootstrapTenantId <= 0) {
            throw new IllegalStateException("AUTH_BOOTSTRAP_TENANT_ID must identify a trusted bootstrap tenant");
        }
    }

    private void requireBootstrapPassword() {
        if (bootstrapSystemPassword == null || bootstrapSystemPassword.isBlank()) {
            throw new IllegalStateException("AUTH_BOOTSTRAP_SYSTEM_PASSWORD is required to create system_admin");
        }
    }

    private void runAsBootstrapAccount(Long accountId, String username, Runnable action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext bootstrapContext = SecurityContextHolder.createEmptyContext();
        TenantAccountUserDetails principal = new TenantAccountUserDetails(
                accountId,
                bootstrapTenantId,
                username,
                "",
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))
        );
        bootstrapContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(bootstrapContext);
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private AuthRole createRole(RoleRepository roleRepository, String roleName) {
        AuthRole role = new AuthRole();
        role.setName(roleName);
        role.setRoleCode(roleName);
        role.setDescription("Platform system administrator");
        role.setActive(true);
        role.setCreatedBy(0L);
        role.setUpdatedBy(0L);
        return roleRepository.save(role);
    }

    private void seedDefaultWebClient(ClientApplicationService clientApplicationService,
                                      ClientPermissionService clientPermissionService,
                                      ClientApiRegistryService clientApiRegistryService,
                                      Set<String> privilegeCodes) {
        ClientApplicationRequestDto requestDto = new ClientApplicationRequestDto();
        requestDto.setClientCode("WEB");
        requestDto.setClientName("Default Web Application");
        requestDto.setClientType(ClientApplicationType.WEB);
        requestDto.setStatus(ClientApplicationStatus.ACTIVE);
        requestDto.setAllowedOrigins("http://localhost:4200,http://localhost:4300,http://localhost:5300");
        requestDto.setDescription("Default first-party web frontend client.");
        clientApplicationService.save(requestDto, "system_admin");

        ClientPermissionAssignmentRequestDto featureAssignment = new ClientPermissionAssignmentRequestDto();
        featureAssignment.setClientCode("WEB");
        featureAssignment.setPrivilegeCodes(privilegeCodes);
        clientPermissionService.assignFeaturePermissions(featureAssignment, "system_admin");

        ApiRegistrySyncReportDto syncReport = clientApiRegistryService.syncFromAnnotations("system_admin");
        if (syncReport.getConflicted() > 0) {
            throw new IllegalStateException("API registry synchronization has unresolved conflicts: "
                    + syncReport.getConflicts());
        }

        Set<Long> apiRegistryIds = syncReport.getRecords().stream()
                .filter(ApiRegistryDto::isActive)
                .filter(api -> "ANNOTATION".equals(api.getSource()))
                .filter(api -> !api.isPublicApi())
                .map(ApiRegistryDto::getId)
                .collect(Collectors.toSet());
        ClientPermissionAssignmentRequestDto apiAssignment = new ClientPermissionAssignmentRequestDto();
        apiAssignment.setClientCode("WEB");
        apiAssignment.setApiRegistryIds(apiRegistryIds);
        clientPermissionService.assignApiPermissions(apiAssignment, "system_admin");

        ClientPermissionAssignmentRequestDto webTenants = new ClientPermissionAssignmentRequestDto();
        webTenants.setClientCode("WEB");
        webTenants.setTenantIds(Set.of(bootstrapTenantId));
        clientPermissionService.assignTenants(webTenants, "system_admin");

        ClientPermissionAssignmentRequestDto systemAdminTenants = new ClientPermissionAssignmentRequestDto();
        systemAdminTenants.setClientCode("SYSTEM_ADMIN_WEB");
        systemAdminTenants.setTenantIds(Set.of(bootstrapTenantId));
        clientPermissionService.assignTenants(systemAdminTenants, "system_admin");

        seedSystemAdminBackupPermissions(clientPermissionService, syncReport);
        seedSystemAdminUserRolePermissions(clientPermissionService, syncReport);
        seedSystemAdminTenantPermissions(clientPermissionService, syncReport);
        seedSystemAdminPlatformPermissions(clientPermissionService, syncReport);
        seedSystemAdminSessionLifecyclePermissions(clientPermissionService, syncReport);
    }

    private void seedSystemAdminSessionLifecyclePermissions(ClientPermissionService clientPermissionService,
                                                            ApiRegistrySyncReportDto syncReport) {
        Set<String> lifecyclePaths = Set.of(
                "/api/v1/auth/application-context",
                "/api/v1/auth/session-status",
                "/api/v1/auth/logout"
        );
        Set<Long> lifecycleApiRegistryIds = syncReport.getRecords().stream()
                .filter(ApiRegistryDto::isActive)
                .filter(api -> lifecyclePaths.contains(api.getPathPattern()))
                .map(ApiRegistryDto::getId)
                .collect(Collectors.toSet());
        ClientPermissionAssignmentRequestDto apiAssignment = new ClientPermissionAssignmentRequestDto();
        apiAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        apiAssignment.setApiRegistryIds(lifecycleApiRegistryIds);
        clientPermissionService.grantApiPermissions(apiAssignment, "system_admin");
    }

    private void seedSystemAdminPlatformPermissions(ClientPermissionService clientPermissionService,
                                                    ApiRegistrySyncReportDto syncReport) {
        ClientPermissionAssignmentRequestDto featureAssignment = new ClientPermissionAssignmentRequestDto();
        featureAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        featureAssignment.setPrivilegeCodes(Set.of(
                BootstrapAdministrationPrivileges.CLIENT_APPLICATION_VIEW,
                BootstrapAdministrationPrivileges.CLIENT_APPLICATION_MANAGE,
                BootstrapAdministrationPrivileges.CLIENT_CREDENTIAL_ROTATE,
                BootstrapAdministrationPrivileges.CLIENT_API_PERMISSION_ASSIGN,
                BootstrapAdministrationPrivileges.CLIENT_FEATURE_PERMISSION_ASSIGN,
                BootstrapAdministrationPrivileges.CLIENT_TENANT_ASSIGN,
                BootstrapAdministrationPrivileges.API_REGISTRY_VIEW,
                BootstrapAdministrationPrivileges.API_REGISTRY_MANAGE,
                BootstrapAdministrationPrivileges.API_REGISTRY_SYNCHRONIZE,
                BootstrapAdministrationPrivileges.PRIVILEGE_CATALOG_VIEW,
                BootstrapAdministrationPrivileges.PRIVILEGE_CATALOG_SYNCHRONIZE,
                BootstrapAdministrationPrivileges.PRIVILEGE_CATALOG_ASSIGN,
                BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_VIEW,
                BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_MANAGE,
                BootstrapAdministrationPrivileges.WORKFLOW_ADMINISTRATION_VIEW,
                BootstrapAdministrationPrivileges.WORKFLOW_ADMINISTRATION_MANAGE,
                BootstrapAdministrationPrivileges.LICENSE_ADMINISTRATION_VIEW,
                BootstrapAdministrationPrivileges.LICENSE_ADMINISTRATION_MANAGE
        ));
        clientPermissionService.grantFeaturePermissions(featureAssignment, "system_admin");

        Set<Long> platformApiRegistryIds = syncReport.getRecords().stream()
                .filter(ApiRegistryDto::isActive)
                .filter(api -> api.getPathPattern() != null)
                .filter(api -> api.getPathPattern().startsWith("/api/v1/system/client-app")
                        || api.getPathPattern().startsWith("/api/v1/system/api-registry")
                        || api.getPathPattern().startsWith("/api/v1/system/privilege")
                        || api.getPathPattern().startsWith("/api/v1/system/layout")
                        || api.getPathPattern().startsWith("/api/v1/system/workflow")
                        || api.getPathPattern().startsWith("/api/v1/system/license"))
                .map(ApiRegistryDto::getId)
                .collect(Collectors.toSet());
        ClientPermissionAssignmentRequestDto apiAssignment = new ClientPermissionAssignmentRequestDto();
        apiAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        apiAssignment.setApiRegistryIds(platformApiRegistryIds);
        clientPermissionService.grantApiPermissions(apiAssignment, "system_admin");
    }

    private void seedSystemAdminBackupPermissions(ClientPermissionService clientPermissionService,
                                                  ApiRegistrySyncReportDto syncReport) {
        ClientPermissionAssignmentRequestDto featureAssignment = new ClientPermissionAssignmentRequestDto();
        featureAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        featureAssignment.setPrivilegeCodes(Set.of(
                BootstrapAdministrationPrivileges.DATABASE_BACKUP_VIEW,
                BootstrapAdministrationPrivileges.DATABASE_BACKUP_EXECUTE,
                BootstrapAdministrationPrivileges.DATABASE_BACKUP_DOWNLOAD,
                BootstrapAdministrationPrivileges.DATABASE_BACKUP_DELIVER,
                BootstrapAdministrationPrivileges.DATABASE_BACKUP_MANAGE
        ));
        clientPermissionService.grantFeaturePermissions(featureAssignment, "system_admin");

        Set<Long> backupApiRegistryIds = syncReport.getRecords().stream()
                .filter(ApiRegistryDto::isActive)
                .filter(api -> api.getPathPattern() != null)
                .filter(api -> api.getPathPattern().startsWith("/api/v1/system/backup"))
                .map(ApiRegistryDto::getId)
                .collect(Collectors.toSet());
        ClientPermissionAssignmentRequestDto apiAssignment = new ClientPermissionAssignmentRequestDto();
        apiAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        apiAssignment.setApiRegistryIds(backupApiRegistryIds);
        clientPermissionService.grantApiPermissions(apiAssignment, "system_admin");
    }

    private void seedSystemAdminUserRolePermissions(ClientPermissionService clientPermissionService,
                                                     ApiRegistrySyncReportDto syncReport) {
        ClientPermissionAssignmentRequestDto featureAssignment = new ClientPermissionAssignmentRequestDto();
        featureAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        featureAssignment.setPrivilegeCodes(Set.of(
                BootstrapAdministrationPrivileges.USER_ADMINISTRATION_VIEW,
                BootstrapAdministrationPrivileges.USER_ADMINISTRATION_MANAGE,
                BootstrapAdministrationPrivileges.USER_ADMINISTRATION_ASSIGN,
                BootstrapAdministrationPrivileges.ROLE_ADMINISTRATION_VIEW,
                BootstrapAdministrationPrivileges.ROLE_ADMINISTRATION_MANAGE
        ));
        clientPermissionService.grantFeaturePermissions(featureAssignment, "system_admin");

        Set<Long> userRoleApiRegistryIds = syncReport.getRecords().stream()
                .filter(ApiRegistryDto::isActive)
                .filter(api -> api.getPathPattern() != null)
                .filter(api -> api.getPathPattern().startsWith("/api/v1/system/user")
                        || api.getPathPattern().startsWith("/api/v1/system/role"))
                .map(ApiRegistryDto::getId)
                .collect(Collectors.toSet());
        ClientPermissionAssignmentRequestDto apiAssignment = new ClientPermissionAssignmentRequestDto();
        apiAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        apiAssignment.setApiRegistryIds(userRoleApiRegistryIds);
        clientPermissionService.grantApiPermissions(apiAssignment, "system_admin");
    }

    private void seedSystemAdminTenantPermissions(ClientPermissionService clientPermissionService,
                                                  ApiRegistrySyncReportDto syncReport) {
        ClientPermissionAssignmentRequestDto featureAssignment = new ClientPermissionAssignmentRequestDto();
        featureAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        featureAssignment.setPrivilegeCodes(Set.of(
                BootstrapAdministrationPrivileges.TENANT_VIEW,
                BootstrapAdministrationPrivileges.TENANT_REGISTER,
                BootstrapAdministrationPrivileges.TENANT_DOMAIN_VERIFY,
                BootstrapAdministrationPrivileges.TENANT_LIFECYCLE_MANAGE
        ));
        clientPermissionService.grantFeaturePermissions(featureAssignment, "system_admin");

        Set<Long> tenantApiRegistryIds = syncReport.getRecords().stream()
                .filter(ApiRegistryDto::isActive)
                .filter(api -> api.getPathPattern() != null)
                .filter(api -> api.getPathPattern().startsWith("/api/v1/system/tenants"))
                .map(ApiRegistryDto::getId)
                .collect(Collectors.toSet());
        ClientPermissionAssignmentRequestDto apiAssignment = new ClientPermissionAssignmentRequestDto();
        apiAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        apiAssignment.setApiRegistryIds(tenantApiRegistryIds);
        clientPermissionService.grantApiPermissions(apiAssignment, "system_admin");
    }

    private void seedDefaultAuthPolicies(AuthClientAuthPolicyRepository authPolicyRepository,
                                         AuthenticationProperties authenticationProperties) {
        seedClientAuthPolicy(authPolicyRepository, "WEB", authenticationProperties);

        seedClientAuthPolicy(authPolicyRepository, "nexacore-client", authenticationProperties);

        seedClientAuthPolicy(authPolicyRepository, "privilege-frontend", authenticationProperties);
    }

    private void seedPersonRoutePolicies(LayoutRoutePolicyService routePolicyService) {
        routePolicyService.synchronizePolicy("WEB", "/person", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW)
        ), 0L);
        routePolicyService.synchronizePolicy("WEB", "/person/create", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.CREATE)
        ), 0L);
        routePolicyService.synchronizePolicy("WEB", "/person/:id/edit", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.UPDATE)
        ), 0L);
        routePolicyService.synchronizePolicy("WEB", "/person/:id/preview", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW)
        ), 0L);
    }

    private void seedPersonUiPolicies(LayoutUiPolicyService uiPolicyService) {
        uiPolicyService.synchronizePolicy("WEB", "person.list.add-button", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.CREATE)
        ), 0L);
        uiPolicyService.synchronizePolicy("WEB", "person.list.edit-button", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.UPDATE)
        ), 0L);
        uiPolicyService.synchronizePolicy("WEB", "person.list.delete-button", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.DELETE)
        ), 0L);
        uiPolicyService.synchronizePolicy("WEB", "person.list.preview-button", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW)
        ), 0L);
        uiPolicyService.synchronizePolicy("WEB", "person.preview.edit-button", PrivilegeMatchMode.ANY, Set.of(
                code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.UPDATE)
        ), 0L);
    }

    private void seedClientAuthPolicy(AuthClientAuthPolicyRepository authPolicyRepository,
                                      String clientCode,
                                      AuthenticationProperties authenticationProperties) {
        LoginMethod enabledLoginMethod = authenticationProperties.getLoginMethods(clientCode).stream()
                .findFirst()
                .orElse(LoginMethod.PASSWORD);
        LoginIdentifierType loginIdentifierType = authenticationProperties.getLoginIdentifiers(clientCode).stream()
                .findFirst()
                .orElse(LoginIdentifierType.USERNAME);

        seedClientAuthPolicy(authPolicyRepository, clientCode, enabledLoginMethod, loginIdentifierType);
    }

    private void seedClientAuthPolicy(AuthClientAuthPolicyRepository authPolicyRepository,
                                      String clientCode,
                                      LoginMethod enabledLoginMethod,
                                      LoginIdentifierType loginIdentifierType) {
        List<AuthClientAuthPolicy> policies = authPolicyRepository.findByClientCodeIgnoreCase(clientCode);
        AuthClientAuthPolicy selectedPolicy = null;

        for (AuthClientAuthPolicy policy : policies) {
            boolean selected = enabledLoginMethod.equals(policy.getLoginMethod())
                    && loginIdentifierType.equals(policy.getLoginIdentifierType());
            policy.setEnabled(selected);
            policy.setUpdatedBy(0L);
            if (selected) {
                selectedPolicy = policy;
            }
        }

        if (selectedPolicy == null) {
            selectedPolicy = AuthClientAuthPolicy.builder()
                    .clientCode(clientCode)
                    .loginMethod(enabledLoginMethod)
                    .loginIdentifierType(loginIdentifierType)
                    .enabled(true)
                    .createdBy(0L)
                    .updatedBy(0L)
                    .build();
            policies.add(selectedPolicy);
        }

        authPolicyRepository.saveAll(policies);
    }

    private Set<String> seedModulePrivileges(SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                             List<ModulePrivilegeProvider> modulePrivilegeProviders) {
        Set<String> privilegeCodes = new HashSet<>();
        modulePrivilegeProviders.stream()
                .flatMap(provider -> provider.getPrivilegeFeatures().stream())
                .forEach(feature -> feature.getActions().forEach(action -> {
                    SysPrivSubMenu subMenu = seedSubMenuIfMissing(systemPrivilegeRegistryService, feature);
                    SysPrivPrivilege privilege = savePrivilegeIfMissing(
                            systemPrivilegeRegistryService,
                            feature,
                            action.getActionCode(),
                            action.getActionName(),
                            subMenu
                    );
                    privilegeCodes.add(privilege.getPrivilegeCode());
                }));
        return privilegeCodes;
    }

    private SysPrivSubMenu seedSubMenuIfMissing(SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                         PrivilegeFeatureDefinitionDto feature) {
        PrivilegeMenuItemDto menuItem = feature.getMenuItems().stream()
                .findFirst()
                .orElse(null);

        String name = menuItem == null ? feature.getMenuLabel() : menuItem.getLabel();
        String url = menuItem == null ? "/" + feature.getFeatureName().toLowerCase().replace(" ", "-") : menuItem.getPath();
        String icon = menuItem == null ? feature.getIcon() : menuItem.getIcon();

        SysPrivSubMenu subMenu = systemPrivilegeRegistryService.findSubMenu(
                        feature.getModuleCode(),
                        feature.getSubmoduleCode(),
                        feature.getFeatureTypeCode(),
                        feature.getFeatureCode(),
                        url
                )
                .orElseGet(SysPrivSubMenu::new);

        subMenu.setName(name == null ? feature.getFeatureName() : name);
        subMenu.setUrl(url);
        subMenu.setIcon(icon);
        subMenu.setModuleCode(feature.getModuleCode());
        subMenu.setModuleName(feature.getModuleName());
        subMenu.setSubmoduleCode(feature.getSubmoduleCode());
        subMenu.setSubmoduleName(feature.getSubmoduleName());
        subMenu.setFeatureTypeCode(feature.getFeatureTypeCode());
        subMenu.setFeatureTypeName(feature.getFeatureTypeName());
        subMenu.setFeatureCode(feature.getFeatureCode());
        subMenu.setFeatureName(feature.getFeatureName());
        subMenu.setActive(true);
        if (subMenu.getCreatedBy() == null) {
            subMenu.setCreatedBy(0L);
        }
        subMenu.setUpdatedBy(0L);

        return systemPrivilegeRegistryService.saveSubMenu(subMenu);
    }

    private SysPrivPrivilege savePrivilegeIfMissing(SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                             PrivilegeFeatureDefinitionDto feature,
                                             String actionCode,
                                             String actionName,
                                             SysPrivSubMenu subMenu) {
        String privilegeCode = feature.getModuleCode() + feature.getSubmoduleCode() + feature.getFeatureTypeCode() + feature.getFeatureCode() + actionCode;

        SysPrivPrivilege privilege = systemPrivilegeRegistryService.findPrivilegeByCode(privilegeCode)
                .orElseGet(() -> SysPrivPrivilege.builder()
                        .privilegeCode(privilegeCode)
                        .moduleCode(feature.getModuleCode())
                        .moduleName(feature.getModuleName())
                        .submoduleCode(feature.getSubmoduleCode())
                        .submoduleName(feature.getSubmoduleName())
                        .featureTypeCode(feature.getFeatureTypeCode())
                        .featureTypeName(feature.getFeatureTypeName())
                        .featureCode(feature.getFeatureCode())
                        .featureName(feature.getFeatureName())
                        .actionCode(actionCode)
                        .actionName(actionName)
                        .active(true)
                        .build());

        privilege.setSubMenu(subMenu);
        privilege.setActive(true);
        return systemPrivilegeRegistryService.savePrivilege(privilege);
    }

    private void assignRolePrivileges(SystemPrivilegeRegistryService systemPrivilegeRegistryService, AuthRole role, Set<String> privilegeCodes) {
        systemPrivilegeRegistryService.assignRolePrivileges(role.getId(), privilegeCodes);
    }

    private Set<String> privilegesForModule(Set<String> privilegeCodes, ApplicationModule module) {
        return privilegeCodes.stream()
                .filter(code -> code != null && code.startsWith(module.getCode()))
                .collect(Collectors.toSet());
    }

    private String code(ApplicationModule applicationModule,
                        ApplicationSubmodule applicationSubmodule,
                        FeatureType featureType,
                        String featureCode,
                        PrivilegeAction privilegeAction) {
        return applicationModule.getCode() + applicationSubmodule.getCode() + featureType.getCode() + featureCode + privilegeAction.getCode();
    }
}
