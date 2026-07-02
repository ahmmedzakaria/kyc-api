package com.nexacore.servicesmodule.reportservice.provider.interfaces;

import com.nexacore.servicesmodule.reportservice.dto.ReportRequest;
import com.nexacore.servicesmodule.reportservice.dto.ReportResponse;

public interface ReportProvider {

    String providerName();

    ReportResponse render(ReportRequest request);
}
