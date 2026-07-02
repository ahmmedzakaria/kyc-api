package com.nexacore.servicesmodule.reportservice.provider.implementations;

import com.nexacore.servicesmodule.reportservice.config.ReportServiceProperties;
import com.nexacore.servicesmodule.reportservice.dto.ReportRequest;
import com.nexacore.servicesmodule.reportservice.dto.ReportResponse;
import com.nexacore.servicesmodule.reportservice.enums.ReportFormat;
import com.nexacore.servicesmodule.reportservice.provider.interfaces.ReportProvider;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JasperReportProvider implements ReportProvider {

    private static final String PROVIDER_NAME = "jasper";

    private final ReportServiceProperties properties;
    private final ResourceLoader resourceLoader;

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public ReportResponse render(ReportRequest request) {
        validateRequest(request);

        try {
            JasperReport jasperReport = compileReport(request.getTemplatePath());
            JasperPrint jasperPrint = fillReport(jasperReport, request);
            byte[] content = exportReport(jasperPrint, request.getFormat());
            String fileName = buildFileName(request);

            return ReportResponse.builder()
                    .success(true)
                    .providerName(providerName())
                    .reportName(request.getReportName())
                    .format(request.getFormat())
                    .fileName(fileName)
                    .contentType(request.getFormat().getContentType())
                    .content(content)
                    .size(content.length)
                    .metadata(request.getMetadata())
                    .build();
        } catch (IOException | JRException e) {
            throw new IllegalStateException("Failed to render Jasper report", e);
        }
    }

    private JasperReport compileReport(String templatePath) throws IOException, JRException {
        Resource resource = resourceLoader.getResource(resolveTemplateLocation(templatePath));
        if (!resource.exists()) {
            throw new IllegalArgumentException("Report template not found: " + templatePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return JasperCompileManager.compileReport(inputStream);
        }
    }

    private JasperPrint fillReport(JasperReport jasperReport, ReportRequest request) throws JRException {
        Map<String, Object> parameters = new HashMap<>();
        if (request.getParameters() != null) {
            parameters.putAll(request.getParameters());
        }

        if (CollectionUtils.isEmpty(request.getRows())) {
            return JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
        }

        return JasperFillManager.fillReport(
                jasperReport,
                parameters,
                new JRBeanCollectionDataSource(request.getRows())
        );
    }

    private byte[] exportReport(JasperPrint jasperPrint, ReportFormat format) throws JRException, IOException {
        return switch (format) {
            case PDF -> JasperExportManager.exportReportToPdf(jasperPrint);
            case XLSX -> exportXlsx(jasperPrint);
            case CSV -> exportCsv(jasperPrint);
        };
    }

    private byte[] exportXlsx(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(false);
        configuration.setDetectCellType(true);
        configuration.setCollapseRowSpan(false);

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        return outputStream.toByteArray();
    }

    private byte[] exportCsv(JasperPrint jasperPrint) throws JRException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            JRCsvExporter exporter = new JRCsvExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleWriterExporterOutput(writer));
            exporter.exportReport();
        }
        return outputStream.toByteArray();
    }

    private String resolveTemplateLocation(String templatePath) {
        String trimmedTemplatePath = templatePath.trim();
        if (trimmedTemplatePath.startsWith("classpath:")
                || trimmedTemplatePath.startsWith("file:")
                || trimmedTemplatePath.startsWith("http:")) {
            return trimmedTemplatePath;
        }

        String basePath = normalizeBasePath(properties.getTemplatesBasePath());
        return "classpath:" + basePath + "/" + trimmedTemplatePath;
    }

    private String normalizeBasePath(String basePath) {
        if (!StringUtils.hasText(basePath)) {
            return "reports/templates";
        }
        return basePath.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String buildFileName(ReportRequest request) {
        String baseName = StringUtils.hasText(request.getOutputFileName())
                ? request.getOutputFileName().trim()
                : StringUtils.hasText(request.getReportName())
                ? request.getReportName().trim()
                : properties.getDefaultFileName();

        String sanitizedBaseName = baseName
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (!StringUtils.hasText(sanitizedBaseName)) {
            sanitizedBaseName = properties.getDefaultFileName();
        }

        String extension = "." + request.getFormat().getFileExtension();
        if (sanitizedBaseName.toLowerCase().endsWith(extension)) {
            return sanitizedBaseName;
        }
        return sanitizedBaseName + extension;
    }

    private void validateRequest(ReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Report request must not be null");
        }
        if (!StringUtils.hasText(request.getTemplatePath())) {
            throw new IllegalArgumentException("Report template path must not be blank");
        }
        if (request.getFormat() == null) {
            request.setFormat(ReportFormat.PDF);
        }
    }
}
