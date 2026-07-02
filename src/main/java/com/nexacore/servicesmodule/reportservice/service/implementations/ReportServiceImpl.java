package com.nexacore.servicesmodule.reportservice.service.implementations;

import com.nexacore.servicesmodule.reportservice.config.ReportServiceProperties;
import com.nexacore.servicesmodule.reportservice.dto.ReportRequest;
import com.nexacore.servicesmodule.reportservice.dto.ReportResponse;
import com.nexacore.servicesmodule.reportservice.provider.interfaces.ReportProvider;
import com.nexacore.servicesmodule.reportservice.service.interfaces.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportServiceProperties properties;
    private final List<ReportProvider> reportProviders;

    @Override
    public ReportResponse renderReport(ReportRequest request) {
        return resolveProvider().render(request);
    }

    private ReportProvider resolveProvider() {
        String configuredProvider = StringUtils.hasText(properties.getProviderName())
                ? properties.getProviderName().trim()
                : "jasper";

        return reportProviders.stream()
                .filter(provider -> provider.providerName().equalsIgnoreCase(configuredProvider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No report provider configured for: " + configuredProvider));
    }
}
