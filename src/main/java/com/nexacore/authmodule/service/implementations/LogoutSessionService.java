package com.nexacore.authmodule.service.implementations;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LogoutSessionService {

    private final Map<String, Instant> loginAfterByUsername = new ConcurrentHashMap<>();

    public void login(String username) {
        loginAfterByUsername.put(username, Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public void logout(String username) {
        loginAfterByUsername.remove(username);
    }

    public boolean isSessionActive(String username, Instant issuedAt) {
        Instant loginAfter = loginAfterByUsername.get(username);
        return loginAfter != null && issuedAt != null && !issuedAt.isBefore(loginAfter);
    }

    public boolean isLoggedIn(String username) {
        return loginAfterByUsername.containsKey(username);
    }
}
