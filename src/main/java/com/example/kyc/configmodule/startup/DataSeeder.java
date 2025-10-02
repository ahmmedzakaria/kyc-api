package com.example.kyc.configmodule.startup;

import com.example.kyc.authmodule.enitty.Role;
import com.example.kyc.authmodule.enitty.User;
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
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            // Ensure ROLE_ADMIN exists
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_ADMIN");
                        return roleRepository.save(role);
                    });

            // Ensure admin user exists
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@example.com");
                admin.setMobile("01700000000");
                admin.setName("System Administrator");
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
}

