package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.servicesmodule.cacheservice.service.interfaces.CacheService;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthorizationDataCacheTest {
    private CacheService cacheService;
    private AuthorizationDataCache cache;

    @BeforeEach
    void setUp() {
        cacheService = mock(CacheService.class);
        cache = new AuthorizationDataCache(cacheService, mock(AccessControlMetrics.class));
        ReflectionTestUtils.setField(cache, "ttl", Duration.ofSeconds(30));
    }

    @Test
    void cachesImmutableUserPrivilegeSnapshotWithBoundedTtl() {
        when(cacheService.get(anyString(), eq(AuthorizationDataCache.StringSetSnapshot.class))).thenReturn(Optional.empty());
        Set<String> result = cache.userPrivileges(7L, 3L, () -> Set.of("P1"));
        assertThat(result).containsExactly("P1");
        verify(cacheService).put(eq("authorization:user-privileges:7:3"), any(), eq(Duration.ofSeconds(30)));
    }

    @Test
    void cacheFailureFallsBackToAuthoritativeLoader() {
        when(cacheService.get(anyString(), eq(AuthorizationDataCache.RegistrySnapshot.class)))
                .thenThrow(new IllegalStateException("redis unavailable"));
        AtomicInteger loads = new AtomicInteger();
        List<SysAccApiRegistry> result = cache.registryMappings("GET", () -> {
            loads.incrementAndGet();
            return List.of(SysAccApiRegistry.builder().id(1L).httpMethod("GET").pathPattern("/x").active(true).build());
        });
        assertThat(loads).hasValue(1);
        assertThat(result).hasSize(1);
    }

    @Test
    void clientInvalidationEvictsGrantsAndClientFilteredPrivileges() {
        cache.invalidateClientAfterCommit(9L);
        verify(cacheService).delete("authorization:client-grants:9");
        verify(cacheService).deleteByPattern("authorization:user-privileges:*:9");
    }

    @Test
    void registryAndUserInvalidationsEvictOnlyTheirNamespaces() {
        cache.invalidateRegistryAfterCommit();
        cache.invalidateUserAfterCommit(7L);
        cache.invalidateAllUserPrivilegesAfterCommit();

        verify(cacheService).deleteByPattern("authorization:registry:*");
        verify(cacheService).deleteByPattern("authorization:user-privileges:7:*");
        verify(cacheService).deleteByPattern("authorization:user-privileges:*");
    }
}
