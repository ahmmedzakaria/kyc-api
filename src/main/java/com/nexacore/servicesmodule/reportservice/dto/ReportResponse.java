package com.nexacore.servicesmodule.reportservice.dto;

import com.nexacore.servicesmodule.reportservice.enums.ReportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private boolean success;

    private String providerName;

    private String reportName;

    private ReportFormat format;

    private String fileName;

    private String contentType;

    private byte[] content;

    private long size;

    private Map<String, Object> metadata;
}
