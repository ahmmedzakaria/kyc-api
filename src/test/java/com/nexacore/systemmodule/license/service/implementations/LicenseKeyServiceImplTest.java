package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.systemmodule.license.dto.GeneratedLicenseKeyDto;
import com.nexacore.systemmodule.license.dto.LicenseKeyRequestDto;
import com.nexacore.systemmodule.license.entity.SysLicenseKey;
import com.nexacore.systemmodule.license.entity.SysLicensePlan;
import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.repository.LicenseKeyRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.license.repository.LicenseAuditEventRepository;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class LicenseKeyServiceImplTest {

    private final LicenseKeyRepository keyRepository = mock(LicenseKeyRepository.class);
    private final LicenseSubscriptionRepository subscriptionRepository = mock(LicenseSubscriptionRepository.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final LicenseAuditEventRepository auditRepository = mock(LicenseAuditEventRepository.class);
    private final LicenseKeyServiceImpl service = new LicenseKeyServiceImpl(
            keyRepository, subscriptionRepository, dataScopeService, auditRepository);

    @Test
    void generatesRawKeyOnceAndStoresOnlyHash() {
        SysLicenseSubscription subscription = SysLicenseSubscription.builder()
                .id(10L)
                .subscriptionCode("SUB-001")
                .licensePlan(SysLicensePlan.builder().id(1L).planCode("PLAN").build())
                .build();

        when(dataScopeService.requireEffectiveTenant(null)).thenReturn(1L);
        when(subscriptionRepository.findBySubscriptionCodeAndTenantId("SUB-001", 1L))
                .thenReturn(Optional.of(subscription));
        when(keyRepository.save(any(SysLicenseKey.class))).thenAnswer(invocation -> {
            SysLicenseKey key = invocation.getArgument(0, SysLicenseKey.class);
            key.setId(99L);
            return key;
        });

        GeneratedLicenseKeyDto generated = service.generateLicenseKey(LicenseKeyRequestDto.builder()
                .subscriptionCode("SUB-001")
                .build());

        ArgumentCaptor<SysLicenseKey> keyCaptor = ArgumentCaptor.forClass(SysLicenseKey.class);
        verify(keyRepository).save(keyCaptor.capture());

        assertThat(generated.rawLicenseKey()).startsWith("NC-SUB-001-");
        assertThat(keyCaptor.getValue().getLicenseKeyHash()).isNotEqualTo(generated.rawLicenseKey());
        assertThat(keyCaptor.getValue().getKeyPrefix()).isEqualTo(generated.rawLicenseKey().substring(0, 12));
    }

    @Test
    void revokesKeyImmediatelyAndWritesAuditWithoutReturningRawKey() {
        SysLicenseSubscription subscription = SysLicenseSubscription.builder().id(10L).tenantId(1L)
                .subscriptionCode("SUB-001").licensePlan(SysLicensePlan.builder().id(1L).planCode("PLAN").build()).build();
        SysLicenseKey key = SysLicenseKey.builder().id(99L).licenseSubscription(subscription).keyPrefix("NC-SUB-001-").active(true).build();
        when(dataScopeService.requireEffectiveTenant(null)).thenReturn(1L);
        when(keyRepository.findByIdAndLicenseSubscriptionTenantId(99L, 1L)).thenReturn(Optional.of(key));

        var summary = service.revoke(99L, "compromised", 7L);

        assertThat(summary.active()).isFalse();
        assertThat(summary.revokedAt()).isNotNull();
        verify(keyRepository).saveAndFlush(key);
        verify(auditRepository).save(any());
    }
}
