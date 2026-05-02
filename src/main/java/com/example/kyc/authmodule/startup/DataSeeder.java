package com.example.kyc.authmodule.startup;

import com.example.kyc.authmodule.dto.PrivilegeFeatureDefinitionDto;
import com.example.kyc.authmodule.entity.Privilege;
import com.example.kyc.authmodule.entity.Role;
import com.example.kyc.authmodule.entity.User;
import com.example.kyc.authmodule.repository.PrivilegeRepository;
import com.example.kyc.authmodule.repository.RoleRepository;
import com.example.kyc.authmodule.repository.UserRepository;
import com.example.kyc.authmodule.service.interfaces.ModulePrivilegeProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   PrivilegeRepository privilegeRepository,
                                   List<ModulePrivilegeProvider> modulePrivilegeProviders,
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            // Ensure ROLE_ADMIN exists
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_ADMIN");
                        return roleRepository.save(role);
                    });

            seedModulePrivileges(privilegeRepository, adminRole, modulePrivilegeProviders);
            roleRepository.save(adminRole);

            // Ensure admin user exists
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@example.com");
                admin.setMobileNumber("01700000000");
                //admin.setName("System Administrator");
                admin.setPassword(passwordEncoder.encode("admin123")); // BCrypt encoding
                admin.getRoles().add(adminRole);
                admin.setEmailVerified(true);
                admin.setMobileVerified(true);
                admin.setEnabled(true);

                userRepository.save(admin);
                System.out.println("✅ Default admin user created: username=admin, password=admin123");
            } else {
                System.out.println("ℹ️ Admin user already exists, skipping seeding.");
            }
        };
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
}
