package com.example.kyc.authmodule.startup;

import com.example.kyc.authmodule.entity.Privilege;
import com.example.kyc.authmodule.entity.Role;
import com.example.kyc.authmodule.entity.User;
import com.example.kyc.authmodule.enums.ApplicationModule;
import com.example.kyc.authmodule.enums.FeatureType;
import com.example.kyc.authmodule.enums.PrivilegeAction;
import com.example.kyc.authmodule.repository.PrivilegeRepository;
import com.example.kyc.authmodule.repository.RoleRepository;
import com.example.kyc.authmodule.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   PrivilegeRepository privilegeRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            // Ensure ROLE_ADMIN exists
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_ADMIN");
                        return roleRepository.save(role);
                    });

            seedKycPersonOperationPrivileges(privilegeRepository, adminRole);
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

    private void seedKycPersonOperationPrivileges(PrivilegeRepository privilegeRepository, Role adminRole) {
        PrivilegeAction[] actions = {
                PrivilegeAction.CREATE,
                PrivilegeAction.UPDATE,
                PrivilegeAction.DELETE,
                PrivilegeAction.REJECT,
                PrivilegeAction.SEND_BACK,
                PrivilegeAction.VIEW,
                PrivilegeAction.SEARCH
        };

        for (PrivilegeAction action : actions) {
            Privilege privilege = savePrivilegeIfMissing(
                    privilegeRepository,
                    ApplicationModule.KYC,
                    FeatureType.OPERATIONS,
                    "001",
                    "Person",
                    action
            );
            adminRole.getPrivileges().add(privilege);
        }
    }

    private Privilege savePrivilegeIfMissing(PrivilegeRepository privilegeRepository,
                                             ApplicationModule applicationModule,
                                             FeatureType featureType,
                                             String featureCode,
                                             String featureName,
                                             PrivilegeAction action) {
        String privilegeCode = applicationModule.getCode() + featureType.getCode() + featureCode + action.getCode();

        return privilegeRepository.findByPrivilegeCode(privilegeCode)
                .orElseGet(() -> privilegeRepository.save(Privilege.builder()
                        .privilegeCode(privilegeCode)
                        .moduleCode(applicationModule.getCode())
                        .moduleName(applicationModule.getDisplayName())
                        .featureTypeCode(featureType.getCode())
                        .featureTypeName(featureType.getDisplayName())
                        .featureCode(featureCode)
                        .featureName(featureName)
                        .actionCode(action.getCode())
                        .actionName(action.getDisplayName())
                        .active(true)
                        .build()));
    }
}
