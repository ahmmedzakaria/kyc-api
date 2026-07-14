package com.nexacore.systemmodule.license.service.interfaces;

import com.nexacore.systemmodule.license.dto.GeneratedLicenseKeyDto;
import com.nexacore.systemmodule.license.dto.LicenseKeyRequestDto;

public interface LicenseKeyService {
    GeneratedLicenseKeyDto generateLicenseKey(LicenseKeyRequestDto request);

    boolean activateLicenseKey(LicenseKeyRequestDto request);

    boolean validateLicenseKey(LicenseKeyRequestDto request);
}
