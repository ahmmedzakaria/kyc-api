package com.nexacore.servicesmodule.reportservice.dto;

import com.nexacore.servicesmodule.reportservice.enums.ReportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    private String reportName;

    private String templatePath;

    @Builder.Default
    private ReportFormat format = ReportFormat.PDF;

    private Map<String, Object> parameters;

    private Collection<?> rows;

    private String outputFileName;

    private Map<String, Object> metadata;
}
