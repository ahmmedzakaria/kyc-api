package com.example.kyc.config;

import com.example.kyc.repository.UserRepository;
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

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(authorities)
                .build();
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
