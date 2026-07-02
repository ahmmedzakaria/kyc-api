package com.nexacore.servicesmodule.reportservice.provider.implementations;

import com.nexacore.servicesmodule.reportservice.config.ReportServiceProperties;
import com.nexacore.servicesmodule.reportservice.dto.ReportRequest;
import com.nexacore.servicesmodule.reportservice.dto.ReportResponse;
import com.nexacore.servicesmodule.reportservice.enums.ReportFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JasperReportProviderTest {

    private JasperReportProvider provider;

    @BeforeEach
    void setUp() {
        ReportServiceProperties properties = new ReportServiceProperties();
        properties.setTemplatesBasePath("reports/templates");
        properties.setDefaultFileName("report");
        provider = new JasperReportProvider(properties, new DefaultResourceLoader());
    }

    @Test
    void renderPdfReport() {
        ReportResponse response = provider.render(buildRequest(ReportFormat.PDF));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getFileName()).isEqualTo("Sample_Report.pdf");
        assertThat(response.getContent()).isNotEmpty();
    }

    @Test
    void renderXlsxReport() {
        ReportResponse response = provider.render(buildRequest(ReportFormat.XLSX));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getFileName()).isEqualTo("Sample_Report.xlsx");
        assertThat(response.getContent()).isNotEmpty();
    }

    @Test
    void renderCsvReport() {
        ReportResponse response = provider.render(buildRequest(ReportFormat.CSV));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContentType()).isEqualTo("text/csv");
        assertThat(response.getFileName()).isEqualTo("Sample_Report.csv");
        assertThat(response.getContent()).isNotEmpty();
    }

    private ReportRequest buildRequest(ReportFormat format) {
        return ReportRequest.builder()
                .reportName("Sample Report")
                .templatePath("sample_summary_report.jrxml")
                .format(format)
                .parameters(Map.of("REPORT_TITLE", "Sample Report"))
                .rows(List.of(
                        new SummaryRow("Sales", "1200"),
                        new SummaryRow("Refunds", "50")
                ))
                .build();
    }

    public static class SummaryRow {
        private final String label;
        private final String value;

        public SummaryRow(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }
}
