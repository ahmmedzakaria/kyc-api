package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.servicesmodule.cacheservice.service.interfaces.CacheService;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationDataCache {
    private static final String REGISTRY = "authorization:registry:";
    private static final String CLIENT_GRANTS = "authorization:client-grants:";
    private static final String USER_PRIVILEGES = "authorization:user-privileges:";

    private final CacheService cacheService;

    @Value("${access-control.authorization-cache.ttl:PT30S}")
    private Duration ttl;

    public List<SysPrivApiRegistry> registryMappings(String method, Supplier<List<SysPrivApiRegistry>> loader) {
        String key = REGISTRY + method.toUpperCase();
        RegistrySnapshot snapshot = get(key, RegistrySnapshot.class);
        if (snapshot != null) return Arrays.stream(snapshot.getMappings()).map(ApiSnapshot::toEntity).toList();
        List<SysPrivApiRegistry> loaded = List.copyOf(loader.get());
        put(key, new RegistrySnapshot(loaded.stream().map(ApiSnapshot::from).toArray(ApiSnapshot[]::new)));
        return loaded;
    }

    public ClientGrantSnapshot clientGrants(Long clientId, Supplier<ClientGrantSnapshot> loader) {
        String key = CLIENT_GRANTS + clientId;
        ClientGrantSnapshot cached = get(key, ClientGrantSnapshot.class);
        if (cached != null) return cached;
        ClientGrantSnapshot loaded = loader.get();
        put(key, loaded);
        return loaded;
    }

    public Set<String> userPrivileges(Long userId, Long clientId, Supplier<Set<String>> loader) {
        String key = USER_PRIVILEGES + userId + ":" + (clientId == null ? "none" : clientId);
        StringSetSnapshot cached = get(key, StringSetSnapshot.class);
        if (cached != null) return Set.copyOf(Arrays.asList(cached.getValues()));
        Set<String> loaded = Set.copyOf(loader.get());
        put(key, new StringSetSnapshot(loaded.toArray(String[]::new)));
        return loaded;
    }

    public void invalidateRegistryAfterCommit() { afterCommit(() -> deletePattern(REGISTRY + "*")); }
    public void invalidateClientAfterCommit(Long clientId) {
        afterCommit(() -> {
            delete(CLIENT_GRANTS + clientId);
            deletePattern(USER_PRIVILEGES + "*:" + clientId);
        });
    }
    public void invalidateUserAfterCommit(Long userId) { afterCommit(() -> deletePattern(USER_PRIVILEGES + userId + ":*")); }
    public void invalidateAllUserPrivilegesAfterCommit() { afterCommit(() -> deletePattern(USER_PRIVILEGES + "*")); }

    private <T> T get(String key, Class<T> type) {
        try { return cacheService.get(key, type).orElse(null); }
        catch (RuntimeException ex) { log.warn("Authorization cache read failed for {}; using database", key, ex); return null; }
    }
    private void put(String key, Object value) {
        try { cacheService.put(key, value, ttl); }
        catch (RuntimeException ex) { log.warn("Authorization cache write failed for {}", key, ex); }
    }
    private void delete(String key) {
        try { cacheService.delete(key); }
        catch (RuntimeException ex) { log.warn("Authorization cache invalidation failed for {}", key, ex); }
    }
    private void deletePattern(String pattern) {
        try { cacheService.deleteByPattern(pattern); }
        catch (RuntimeException ex) { log.warn("Authorization cache invalidation failed for {}", pattern, ex); }
    }
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else action.run();
    }

    @Data
    public static class RegistrySnapshot {
        private ApiSnapshot[] mappings = new ApiSnapshot[0];
        public RegistrySnapshot() {}
        public RegistrySnapshot(ApiSnapshot[] mappings) { this.mappings = mappings; }
    }
    @Data
    public static class StringSetSnapshot {
        private String[] values = new String[0];
        public StringSetSnapshot() {}
        public StringSetSnapshot(String[] values) { this.values = values; }
    }
    @Data
    public static class ClientGrantSnapshot {
        private Long[] apiRegistryIds = new Long[0];
        private String[] privilegeCodes = new String[0];
        public ClientGrantSnapshot() {}
        public ClientGrantSnapshot(Set<Long> apiRegistryIds, Set<String> privilegeCodes) {
            this.apiRegistryIds = apiRegistryIds.toArray(Long[]::new);
            this.privilegeCodes = privilegeCodes.toArray(String[]::new);
        }
        public Set<Long> apiIds() { return Set.copyOf(Arrays.asList(apiRegistryIds)); }
        public Set<String> privileges() { return Set.copyOf(Arrays.asList(privilegeCodes)); }
    }
    @Data
    public static class ApiSnapshot {
        private Long id; private String apiCode; private String httpMethod; private String pathPattern;
        private String requiredPrivilegeCode; private boolean publicApi; private String clientAuthenticationRequirement;
        private String userAuthorizationRequirement; private String dataScope; private int priority; private boolean active;
        public static ApiSnapshot from(SysPrivApiRegistry api) {
            ApiSnapshot value = new ApiSnapshot();
            value.id=api.getId(); value.apiCode=api.getApiCode(); value.httpMethod=api.getHttpMethod();
            value.pathPattern=api.getPathPattern(); value.requiredPrivilegeCode=api.getRequiredPrivilegeCode();
            value.publicApi=api.isPublicApi(); value.clientAuthenticationRequirement=api.getClientAuthenticationRequirement();
            value.userAuthorizationRequirement=api.getUserAuthorizationRequirement(); value.dataScope=api.getDataScope();
            value.priority=api.getPriority(); value.active=api.isActive(); return value;
        }
        public SysPrivApiRegistry toEntity() {
            return SysPrivApiRegistry.builder().id(id).apiCode(apiCode).httpMethod(httpMethod).pathPattern(pathPattern)
                    .requiredPrivilegeCode(requiredPrivilegeCode).publicApi(publicApi)
                    .clientAuthenticationRequirement(clientAuthenticationRequirement)
                    .userAuthorizationRequirement(userAuthorizationRequirement).dataScope(dataScope)
                    .source("CACHE").priority(priority).active(active).build();
        }
    }
}
