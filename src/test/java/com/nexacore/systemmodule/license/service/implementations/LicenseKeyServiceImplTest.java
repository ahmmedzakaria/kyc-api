package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.systemmodule.license.dto.GeneratedLicenseKeyDto;
import com.nexacore.systemmodule.license.dto.LicenseKeyRequestDto;
import com.nexacore.systemmodule.license.entity.SysLicenseKey;
import com.nexacore.systemmodule.license.entity.SysLicensePlan;
import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.repository.LicenseKeyRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LicenseKeyServiceImplTest {

    private final LicenseKeyRepository keyRepository = mock(LicenseKeyRepository.class);
    private final LicenseSubscriptionRepository subscriptionRepository = mock(LicenseSubscriptionRepository.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final LicenseKeyServiceImpl service = new LicenseKeyServiceImpl(
            keyRepository, subscriptionRepository, dataScopeService);

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
}
