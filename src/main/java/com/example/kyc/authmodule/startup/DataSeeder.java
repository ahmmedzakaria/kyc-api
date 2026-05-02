package com.example.kyc.authmodule.startup;

import com.example.kyc.authmodule.dto.PrivilegeFeatureDefinitionDto;
import com.example.kyc.authmodule.entity.Privilege;
import com.example.kyc.authmodule.entity.Role;
import com.example.kyc.authmodule.entity.User;
import com.example.kyc.authmodule.repository.PrivilegeRepository;
import com.example.kyc.authmodule.repository.RoleRepository;
import com.example.kyc.authmodule.repository.UserRepository;
import com.example.kyc.authmodule.service.interfaces.ModulePrivilegeProvider;
import com.example.kyc.commonmodule.enums.ApplicationModule;
import com.example.kyc.commonmodule.enums.FeatureType;
import com.example.kyc.commonmodule.enums.PrivilegeAction;
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
                                   PrivilegeRepository privilegeRepository,
                                   List<ModulePrivilegeProvider> modulePrivilegeProviders,
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_ADMIN"));
            Role kycOperatorRole = roleRepository.findByName("ROLE_KYC_OPERATOR")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_KYC_OPERATOR"));
            Role kycApproverRole = roleRepository.findByName("ROLE_KYC_APPROVER")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_KYC_APPROVER"));
            Role reportViewerRole = roleRepository.findByName("ROLE_REPORT_VIEWER")
                    .orElseGet(() -> createRole(roleRepository, "ROLE_REPORT_VIEWER"));

            seedModulePrivileges(privilegeRepository, adminRole, modulePrivilegeProviders);
            assignRolePrivileges(privilegeRepository, kycOperatorRole, Set.of(
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.CREATE),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.UPDATE),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.SEARCH),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "002", PrivilegeAction.CREATE),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "002", PrivilegeAction.UPDATE),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "002", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "002", PrivilegeAction.SEARCH)
            ));
            assignRolePrivileges(privilegeRepository, kycApproverRole, Set.of(
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.REJECT),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.SEND_BACK),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "001", PrivilegeAction.SEARCH),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "002", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, FeatureType.OPERATIONS, "002", PrivilegeAction.SEARCH)
            ));
            assignRolePrivileges(privilegeRepository, reportViewerRole, Set.of(
                    code(ApplicationModule.KYC, FeatureType.REPORT, "003", PrivilegeAction.VIEW),
                    code(ApplicationModule.KYC, FeatureType.REPORT, "003", PrivilegeAction.SEARCH)
            ));
            roleRepository.save(adminRole);
            roleRepository.save(kycOperatorRole);
            roleRepository.save(kycApproverRole);
            roleRepository.save(reportViewerRole);

            // Create users
            createUserIfNotExists(userRepository, passwordEncoder,
                    "admin", "admin@example.com", "01700000000", "123", adminRole);

            createUserIfNotExists(userRepository, passwordEncoder,
                    "kyc_operator", "operator@example.com", "01700000001", "123", kycOperatorRole);

            createUserIfNotExists(userRepository, passwordEncoder,
                    "kyc_approver", "approver@example.com", "01700000002", "123", kycApproverRole);

            createUserIfNotExists(userRepository, passwordEncoder,
                    "report_user", "report@example.com", "01700000003", "123", reportViewerRole);
        };
    }

    private void createUserIfNotExists(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       String username,
                                       String email,
                                       String mobile,
                                       String password,
                                       Role role) {

        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setMobileNumber(mobile);
            user.setPassword(passwordEncoder.encode(password));

            user.getRoles().add(role);

            user.setEmailVerified(true);
            user.setMobileVerified(true);
            user.setEnabled(true);

            userRepository.save(user);

            System.out.println("✅ User created: " + username + " / " + password);
        } else {
            System.out.println("⚠️ User already exists: " + username);
        }
    }

    private Role createRole(RoleRepository roleRepository, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }

    private void seedModulePrivileges(PrivilegeRepository privilegeRepository,
                                      Role adminRole,
                                      List<ModulePrivilegeProvider> modulePrivilegeProviders) {
        modulePrivilegeProviders.stream()
                .flatMap(provider -> provider.getPrivilegeFeatures().stream())
                .forEach(feature -> feature.getActions().forEach(action -> {
                    Privilege privilege = savePrivilegeIfMissing(privilegeRepository, feature, action.getActionCode(), action.getActionName());
                    adminRole.getPrivileges().add(privilege);
                }));
    }

    private Privilege savePrivilegeIfMissing(PrivilegeRepository privilegeRepository,
                                             PrivilegeFeatureDefinitionDto feature,
                                             String actionCode,
                                             String actionName) {
        String privilegeCode = feature.getModuleCode() + feature.getFeatureTypeCode() + feature.getFeatureCode() + actionCode;

        return privilegeRepository.findByPrivilegeCode(privilegeCode)
                .orElseGet(() -> privilegeRepository.save(Privilege.builder()
                        .privilegeCode(privilegeCode)
                        .moduleCode(feature.getModuleCode())
                        .moduleName(feature.getModuleName())
                        .featureTypeCode(feature.getFeatureTypeCode())
                        .featureTypeName(feature.getFeatureTypeName())
                        .featureCode(feature.getFeatureCode())
                        .featureName(feature.getFeatureName())
                        .actionCode(actionCode)
                        .actionName(actionName)
                        .active(true)
                        .build()));
    }

    private void assignRolePrivileges(PrivilegeRepository privilegeRepository, Role role, Set<String> privilegeCodes) {
        role.setPrivileges(new HashSet<>(privilegeRepository.findByPrivilegeCodeIn(privilegeCodes)));
    }

    private String code(ApplicationModule applicationModule,
                        FeatureType featureType,
                        String featureCode,
                        PrivilegeAction privilegeAction) {
        return applicationModule.getCode() + featureType.getCode() + featureCode + privilegeAction.getCode();
    }
}
