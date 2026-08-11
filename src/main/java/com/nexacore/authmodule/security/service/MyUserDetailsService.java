package com.nexacore.authmodule.security.service;

import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.authmodule.core.service.UsernameNormalizer;
import com.nexacore.authmodule.core.metrics.AuthCutoverComparisonTelemetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UsernameNormalizer usernameNormalizer;
    private final AuthCutoverComparisonTelemetry comparisonTelemetry;

    public TenantAccountUserDetails loadTenantUser(Long tenantId, String username) {
        if (tenantId == null) {
            throw new UsernameNotFoundException("Tenant context is required");
        }
        String normalized = usernameNormalizer.normalize(username);
        var user = userRepository.findByTenantIdAndNormalizedUsername(tenantId, normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Tenant account not found"));
        comparisonTelemetry.compare(user);
        var authorities = user.getRoles().stream()
                .filter(role -> role.isActive()
                        && (role.getTenantId() == null || role.getTenantId().equals(tenantId)))
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
        return new TenantAccountUserDetails(user.getId(), tenantId, user.getUsername(), user.getPassword(),
                user.isEnabled(), !user.isLocked(), authorities);
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Tenant-bound account lookup is required");
    }

//    @Override
//    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
//        log.debug("Attempting to load user by userName: {}", userName);
//
//        User user = userRepository.findByUsername(userName).orElseThrow(() -> {
//                    log.warn("User not found with userName: {}", userName);
//                    return new UsernameNotFoundException("User not found with userName: " + userName);
//                });
//
//        // Get role from user entity and ensure it's prefixed with "ROLE_"
//        String roleName = (user.getRole() != null && user.getRole().getName() != null)
//                ? user.getRole().getName()
//                : "USER";
//
//        String role = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
//
//        // Wrap into a list of authorities
//        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
//
//        log.info("User authenticated: {}, role: {}", userName, role);
//
//        return new org.springframework.security.core.userdetails.User(
//                user.getUsername(),
//                user.getPassword() != null ? user.getPassword() : "",
//                user.isActive(),          // enabled
//                true,                    // accountNonExpired
//                true,                    // credentialsNonExpired
//                true,                    // accountNonLocked
//                authorities
//        );
//    }
}
