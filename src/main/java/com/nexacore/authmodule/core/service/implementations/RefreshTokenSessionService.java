package com.nexacore.authmodule.core.service.implementations;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RefreshTokenSessionService {
    private final Map<String, String> activeTokenIdByUsername = new ConcurrentHashMap<>();

    public void register(String username, String tokenId) {
        activeTokenIdByUsername.put(username, tokenId);
    }

    public boolean rotate(String username, String currentTokenId, String replacementTokenId) {
        AtomicBoolean rotated = new AtomicBoolean(false);
        activeTokenIdByUsername.compute(username, (ignored, activeTokenId) -> {
            if (activeTokenId != null && activeTokenId.equals(currentTokenId)) {
                rotated.set(true);
                return replacementTokenId;
            }
            return activeTokenId;
        });
        return rotated.get();
    }

    public boolean isActive(String username, String tokenId) {
        return tokenId != null && tokenId.equals(activeTokenIdByUsername.get(username));
    }

    public void revoke(String username) {
        activeTokenIdByUsername.remove(username);
    }
}
