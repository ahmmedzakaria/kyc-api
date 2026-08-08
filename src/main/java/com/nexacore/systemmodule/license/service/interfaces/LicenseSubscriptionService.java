package com.nexacore.systemmodule.license.service.interfaces;

import com.nexacore.systemmodule.license.dto.LicenseEntitlementRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseEntitlementResponseDto;
import com.nexacore.systemmodule.license.dto.LicenseSubscriptionRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseSubscriptionResponseDto;

import java.util.List;

public interface LicenseSubscriptionService {
    LicenseSubscriptionResponseDto assignSubscription(LicenseSubscriptionRequestDto request);

    LicenseSubscriptionResponseDto suspend(String subscriptionCode, String reason);

    LicenseSubscriptionResponseDto reactivate(String subscriptionCode);

    LicenseSubscriptionResponseDto cancel(String subscriptionCode);

    void saveEntitlementOverride(LicenseEntitlementRequestDto request);

    List<LicenseEntitlementResponseDto> listSubscriptionEntitlements(String subscriptionCode);

    List<LicenseSubscriptionResponseDto> listSubscriptions();
}
