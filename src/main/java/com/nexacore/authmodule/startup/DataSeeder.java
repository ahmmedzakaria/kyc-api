package com.nexacore.authmodule.startup;

import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeMenuItemDto;
import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.entity.AuthRole;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
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
import com.nexacore.systemmodule.privilege.assignment.repository.UserPrivilegeRepository;
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

@Configuration
public class DataSeeder {

    @Value("${nexacore.auth.bootstrap-tenant-id:0}")
    private long bootstrapTenantId;

    private final UsernameNormalizer usernameNormalizer = new UsernameNormalizer();

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   UserPrivilegeRepository userPrivilegeRepository,
                                   SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                   List<ModulePrivilegeProvider> modulePrivilegeProviders,
                                   PersonModuleGateway personModuleGateway,
                                   ClientApplicationService clientApplicationService,
                                   ClientPermissionService clientPermissionService,
                                   ClientApiRegistryService clientApiRegistryService,
                                   LayoutRoutePolicyService layoutRoutePolicyService,
                                   LayoutUiPolicyService layoutUiPolicyService,
                                   AuthClientAuthPolicyRepository authPolicyRepository,
                                   AuthenticationProperties authenticationProperties,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            AuthRole adminRole = roleRepository.findByTenantIdIsNullAndRoleCodeIgnoreCase("ROLE_ADMIN")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_ADMIN"));
            AuthRole systemAdminRole = roleRepository.findByTenantIdIsNullAndRoleCodeIgnoreCase("ROLE_SYSTEM_ADMIN")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_SYSTEM_ADMIN"));
            AuthRole kycOperatorRole = roleRepository.findByTenantIdIsNullAndRoleCodeIgnoreCase("ROLE_KYC_OPERATOR")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_KYC_OPERATOR"));
            AuthRole kycApproverRole = roleRepository.findByTenantIdIsNullAndRoleCodeIgnoreCase("ROLE_KYC_APPROVER")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_KYC_APPROVER"));
            systemPrivilegeRegistryService.syncApplicationCatalog();
            Set<String> adminPrivilegeCodes = seedModulePrivileges(systemPrivilegeRegistryService, modulePrivilegeProviders);
            Set<String> systemPrivilegeCodes = privilegesForModule(adminPrivilegeCodes, ApplicationModule.SYSTEM);
            Set<String> nonSystemPrivilegeCodes = new HashSet<>(adminPrivilegeCodes);
            nonSystemPrivilegeCodes.removeAll(systemPrivilegeCodes);
            assignRolePrivileges(systemPrivilegeRegistryService, systemAdminRole, systemPrivilegeCodes);
            assignRolePrivileges(systemPrivilegeRegistryService, adminRole, nonSystemPrivilegeCodes);
            assignRolePrivileges(systemPrivilegeRegistryService, kycOperatorRole, Set.of(
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.CREATE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.UPDATE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.DELETE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.SEARCH)
            ));
            assignRolePrivileges(systemPrivilegeRegistryService, kycApproverRole, Set.of(
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.APPROVE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.REJECT),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.SEND_BACK),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.SEARCH)
            ));

            seedDefaultUserWithExactRoles(userRepository, passwordEncoder, personModuleGateway,
                    "admin", "123", "admin@example.com", "01700000000", "Admin", "User", adminRole);

            seedDefaultUserWithExactRoles(userRepository, passwordEncoder, personModuleGateway,
                    "system_admin", "123", "system.admin@example.com", "01700000005",
                    "System", "Administrator", systemAdminRole);

            requireBootstrapTenant();
            Long adminUserId = userRepository.findByTenantIdAndNormalizedUsername(
                            bootstrapTenantId, usernameNormalizer.normalize("admin"))
                    .orElseThrow(() -> new IllegalStateException("Seeded admin user not found"))
                    .getId();
            userPrivilegeRepository.deleteByUserIdAndPrivilegeModuleCode(
                    adminUserId, ApplicationModule.SYSTEM.getCode());

            seedDefaultUser(userRepository, passwordEncoder, personModuleGateway,
                    "kyc_operator", "123", "operator@example.com", "01700000001", "Kyc", "Operator", kycOperatorRole);

            seedDefaultUser(userRepository, passwordEncoder, personModuleGateway,
                    "kyc_approver", "123", "approver@example.com", "01700000002", "Kyc", "Approver", kycApproverRole);

            seedDefaultUser(userRepository, passwordEncoder, personModuleGateway,
                    "kyc_manager", "123", "manager@example.com", "01700000004", "Kyc", "Manager",
                    kycOperatorRole, kycApproverRole);

            seedDefaultWebClient(
                    clientApplicationService,
                    clientPermissionService,
                    clientApiRegistryService,
                    adminPrivilegeCodes
            );

            seedPersonRoutePolicies(layoutRoutePolicyService);
            seedPersonUiPolicies(layoutUiPolicyService);

            seedDefaultAuthPolicies(authPolicyRepository, authenticationProperties);
        };
    }

    private void seedDefaultUser(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 PersonModuleGateway personModuleGateway,
                                 String username,
                                 String password,
                                 String email,
                                 String mobile,
                                 String firstName,
                                 String lastName,
                                 AuthRole... roles) {
        PersonSummaryDto person = personModuleGateway.ensurePersonForUser(username, email, mobile, firstName, lastName);

        requireBootstrapTenant();
        AuthUser existingUser = userRepository.findByTenantIdAndNormalizedUsername(
                bootstrapTenantId, usernameNormalizer.normalize(username)).orElse(null);
        if (existingUser != null) {
            existingUser.setPersonId(person.getId());
            existingUser.getRoles().addAll(List.of(roles));
            userRepository.save(existingUser);
            return;
        }

        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setNormalizedUsername(usernameNormalizer.normalize(username));
        user.setTenantId(bootstrapTenantId);
        user.setPersonId(person.getId());
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(new HashSet<>(List.of(roles)));
        user.setEnabled(true);

        userRepository.save(user);

        System.out.println("Seeded default user: " + username + " / " + password);
    }

    private void seedDefaultUserWithExactRoles(UserRepository userRepository,
                                               PasswordEncoder passwordEncoder,
                                               PersonModuleGateway personModuleGateway,
                                               String username,
                                               String password,
                                               String email,
                                               String mobile,
                                               String firstName,
                                               String lastName,
                                               AuthRole... roles) {
        seedDefaultUser(userRepository, passwordEncoder, personModuleGateway,
                username, password, email, mobile, firstName, lastName, roles);
        AuthUser user = userRepository.findByTenantIdAndNormalizedUsername(
                        bootstrapTenantId, usernameNormalizer.normalize(username))
                .orElseThrow(() -> new IllegalStateException("Seeded user not found: " + username));
        user.setRoles(new HashSet<>(List.of(roles)));
        userRepository.save(user);
    }

    private void requireBootstrapTenant() {
        if (bootstrapTenantId <= 0) {
            throw new IllegalStateException("AUTH_BOOTSTRAP_TENANT_ID must identify a trusted bootstrap tenant");
        }
    }

    private AuthRole createRole(RoleRepository roleRepository, String roleName) {
        AuthRole role = new AuthRole();
        role.setName(roleName);
        role.setRoleCode(roleName);
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
        clientApplicationService.save(requestDto, "admin");

        ClientPermissionAssignmentRequestDto featureAssignment = new ClientPermissionAssignmentRequestDto();
        featureAssignment.setClientCode("WEB");
        featureAssignment.setPrivilegeCodes(privilegeCodes);
        clientPermissionService.assignFeaturePermissions(featureAssignment, "admin");

        ApiRegistrySyncReportDto syncReport = clientApiRegistryService.syncFromAnnotations("admin");
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
        clientPermissionService.assignApiPermissions(apiAssignment, "admin");

        seedSystemAdminBackupPermissions(clientPermissionService, syncReport);
        seedSystemAdminUserRolePermissions(clientPermissionService, syncReport);
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
        clientPermissionService.grantFeaturePermissions(featureAssignment, "admin");

        Set<Long> backupApiRegistryIds = syncReport.getRecords().stream()
                .filter(ApiRegistryDto::isActive)
                .filter(api -> api.getPathPattern() != null)
                .filter(api -> api.getPathPattern().startsWith("/api/v1/system/backup"))
                .map(ApiRegistryDto::getId)
                .collect(Collectors.toSet());
        ClientPermissionAssignmentRequestDto apiAssignment = new ClientPermissionAssignmentRequestDto();
        apiAssignment.setClientCode("SYSTEM_ADMIN_WEB");
        apiAssignment.setApiRegistryIds(backupApiRegistryIds);
        clientPermissionService.grantApiPermissions(apiAssignment, "admin");
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
        clientPermissionService.grantFeaturePermissions(featureAssignment, "admin");

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
        clientPermissionService.grantApiPermissions(apiAssignment, "admin");
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
