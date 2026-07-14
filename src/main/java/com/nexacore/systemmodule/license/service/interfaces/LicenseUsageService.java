package com.nexacore.systemmodule.license.service.interfaces;

import com.nexacore.gatewaymodule.license.dto.LicenseUsageRecordRequestDto;

public interface LicenseUsageService {
    void recordUsage(LicenseUsageRecordRequestDto request);

    long getUsageValue(String subscriptionCode, String usagePeriod, String usageCode);
}
