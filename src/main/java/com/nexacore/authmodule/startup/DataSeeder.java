package com.nexacore.authmodule.startup;

import com.nexacore.systemmodule.privilege.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.dto.PrivilegeMenuItemDto;
import com.nexacore.authmodule.core.entity.AuthRole;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.systemmodule.privilege.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.entity.SysSubMenu;
import com.nexacore.systemmodule.privilege.service.interfaces.ModulePrivilegeProvider;
import com.nexacore.systemmodule.privilege.service.interfaces.SystemPrivilegeRegistryService;
import com.nexacore.systemmodule.privilege.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.enums.FeatureType;
import com.nexacore.systemmodule.privilege.enums.PrivilegeAction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                   List<ModulePrivilegeProvider> modulePrivilegeProviders,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            AuthRole adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_ADMIN"));
            AuthRole kycOperatorRole = roleRepository.findByName("ROLE_KYC_OPERATOR")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_KYC_OPERATOR"));
            AuthRole kycApproverRole = roleRepository.findByName("ROLE_KYC_APPROVER")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_KYC_APPROVER"));
            AuthRole reportViewerRole = roleRepository.findByName("ROLE_REPORT_VIEWER")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_REPORT_VIEWER"));

            Set<String> adminPrivilegeCodes = seedModulePrivileges(systemPrivilegeRegistryService, modulePrivilegeProviders);
            systemPrivilegeRegistryService.assignRolePrivileges(adminRole.getId(), adminPrivilegeCodes);
            assignRolePrivilegesIfMissing(systemPrivilegeRegistryService, kycOperatorRole, Set.of(
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.CREATE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.UPDATE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.SEARCH),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "002", PrivilegeAction.CREATE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "002", PrivilegeAction.UPDATE),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "002", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "002", PrivilegeAction.SEARCH)
            ));
            assignRolePrivilegesIfMissing(systemPrivilegeRegistryService, kycApproverRole, Set.of(
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.REJECT),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.SEND_BACK),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "001", PrivilegeAction.SEARCH),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "002", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.OPERATIONS, "002", PrivilegeAction.SEARCH)
            ));
            assignRolePrivilegesIfMissing(systemPrivilegeRegistryService, reportViewerRole, Set.of(
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.REPORT, "003", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, ApplicationSubmodule.KYC_PERSON, FeatureType.REPORT, "003", PrivilegeAction.SEARCH)
            ));

            seedDefaultUser(userRepository, passwordEncoder,
                    "admin", "123", "admin@example.com", "01700000000", adminRole);

            seedDefaultUser(userRepository, passwordEncoder,
                    "kyc_operator", "123", "operator@example.com", "01700000001", kycOperatorRole);

            seedDefaultUser(userRepository, passwordEncoder,
                    "kyc_approver", "123", "approver@example.com", "01700000002", kycApproverRole);

            seedDefaultUser(userRepository, passwordEncoder,
                    "report_user", "123", "report@example.com", "01700000003", reportViewerRole);

            seedDefaultUser(userRepository, passwordEncoder,
                    "kyc_manager", "123", "manager@example.com", "01700000004",
                    kycOperatorRole, kycApproverRole, reportViewerRole);
        };
    }

    private void seedDefaultUser(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 String username,
                                 String password,
                                 String email,
                                 String mobile,
                                 AuthRole... roles) {

        AuthUser existingUser = userRepository.findByUsername(username).orElse(null);
        if (existingUser != null) {
            existingUser.getRoles().addAll(List.of(roles));
            userRepository.save(existingUser);
            return;
        }

        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(new HashSet<>(List.of(roles)));
        user.setEmailVerified(true);
        user.setMobileVerified(true);
        user.setEnabled(true);

        userRepository.save(user);

        System.out.println("Seeded default user: " + username + " / " + password);
    }

    private AuthRole createRole(RoleRepository roleRepository, String roleName) {
        AuthRole role = new AuthRole();
        role.setName(roleName);
        return roleRepository.save(role);
    }

    private Set<String> seedModulePrivileges(SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                             List<ModulePrivilegeProvider> modulePrivilegeProviders) {
        Set<String> privilegeCodes = new HashSet<>();
        modulePrivilegeProviders.stream()
                .flatMap(provider -> provider.getPrivilegeFeatures().stream())
                .forEach(feature -> feature.getActions().forEach(action -> {
                    SysSubMenu subMenu = seedSubMenuIfMissing(systemPrivilegeRegistryService, feature);
                    SysPrivilege privilege = savePrivilegeIfMissing(
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

    private SysSubMenu seedSubMenuIfMissing(SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                         PrivilegeFeatureDefinitionDto feature) {
        PrivilegeMenuItemDto menuItem = feature.getMenuItems().stream()
                .findFirst()
                .orElse(null);

        String name = menuItem == null ? feature.getMenuLabel() : menuItem.getLabel();
        String url = menuItem == null ? "/" + feature.getFeatureName().toLowerCase().replace(" ", "-") : menuItem.getPath();
        String icon = menuItem == null ? feature.getIcon() : menuItem.getIcon();

        SysSubMenu subMenu = systemPrivilegeRegistryService.findSubMenu(
                        feature.getModuleCode(),
                        feature.getSubmoduleCode(),
                        feature.getFeatureTypeCode(),
                        feature.getFeatureCode(),
                        url
                )
                .orElseGet(SysSubMenu::new);

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

    private SysPrivilege savePrivilegeIfMissing(SystemPrivilegeRegistryService systemPrivilegeRegistryService,
                                             PrivilegeFeatureDefinitionDto feature,
                                             String actionCode,
                                             String actionName,
                                             SysSubMenu subMenu) {
        String privilegeCode = feature.getModuleCode() + feature.getSubmoduleCode() + feature.getFeatureTypeCode() + feature.getFeatureCode() + actionCode;

        SysPrivilege privilege = systemPrivilegeRegistryService.findPrivilegeByCode(privilegeCode)
                .orElseGet(() -> SysPrivilege.builder()
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

    private void assignRolePrivilegesIfMissing(SystemPrivilegeRegistryService systemPrivilegeRegistryService, AuthRole role, Set<String> privilegeCodes) {
        if (systemPrivilegeRegistryService.countRolePrivileges(role.getId()) > 0) {
            return;
        }
        systemPrivilegeRegistryService.assignRolePrivileges(role.getId(), privilegeCodes);
    }

    private String code(ApplicationModule applicationModule,
                        ApplicationSubmodule applicationSubmodule,
                        FeatureType featureType,
                        String featureCode,
                        PrivilegeAction privilegeAction) {
        return applicationModule.getCode() + applicationSubmodule.getCode() + featureType.getCode() + featureCode + privilegeAction.getCode();
    }
}
