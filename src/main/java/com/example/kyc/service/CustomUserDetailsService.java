package com.example.kyc.service;

import com.example.kyc.entity.User;
import com.example.kyc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        log.debug("Attempting to load user by userName: {}", userName);

        User user = userRepository.findByUsername(userName).orElseThrow(() -> {
                    log.warn("User not found with userName: {}", userName);
                    return new UsernameNotFoundException("User not found with userName: " + userName);
                });

        // Get role from user entity and ensure it's prefixed with "ROLE_"
        String roleName = (user.getRole() != null && user.getRole().getName() != null)
                ? user.getRole().getName()
                : "USER";

        String role = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

        // Wrap into a list of authorities
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        log.info("User authenticated: {}, role: {}", userName, role);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword() != null ? user.getPassword() : "",
                user.isActive(),          // enabled
                true,                    // accountNonExpired
                true,                    // credentialsNonExpired
                true,                    // accountNonLocked
                authorities
        );
    }
}
