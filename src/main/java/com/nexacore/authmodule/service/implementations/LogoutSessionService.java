package com.nexacore.authmodule.service.implementations;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LogoutSessionService {

    private final Map<String, Instant> logoutAfterByUsername = new ConcurrentHashMap<>();

    public void logout(String username) {
        logoutAfterByUsername.put(username, Instant.now());
    }

    public boolean isLoggedOut(String username, Instant issuedAt) {
        Instant logoutAfter = logoutAfterByUsername.get(username);
        return logoutAfter != null && issuedAt != null && !issuedAt.isAfter(logoutAfter);
    }
}
