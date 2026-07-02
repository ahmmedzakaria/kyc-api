package com.nexacore.servicesmodule.reportservice.service.interfaces;

import com.nexacore.servicesmodule.reportservice.dto.ReportRequest;
import com.nexacore.servicesmodule.reportservice.dto.ReportResponse;
import com.nexacore.servicesmodule.reportservice.enums.ReportFormat;

import java.util.Collection;
import java.util.Map;

public interface ReportService {

    ReportResponse renderReport(ReportRequest request);

    default ReportResponse renderReport(
            String reportName,
            String templatePath,
            ReportFormat format,
            Map<String, Object> parameters,
            Collection<?> rows
    ) {
        return renderReport(ReportRequest.builder()
                .reportName(reportName)
                .templatePath(templatePath)
                .format(format)
                .parameters(parameters)
                .rows(rows)
                .build());
    }
}
