# Report Service

`reportservice` provides a reusable reporting abstraction for POS and future modules.

## Structure

```text
servicesmodule/reportservice
├── config
├── dto
├── enums
├── provider
│   ├── interfaces
│   └── implementations
└── service
    ├── interfaces
    └── implementations
```

## Flow

```text
POS/reporting module
  -> ReportService
  -> ReportProvider
  -> JasperReportProvider
  -> JRXML template
  -> PDF/XLSX/CSV bytes
```

The caller owns report data, filters, authorization, and business rules. `reportservice` owns template loading, JasperReports compilation, report filling, and export format handling.

## Configuration

```properties
report.service.provider-name=${REPORT_SERVICE_PROVIDER:jasper}
report.service.templates-base-path=${REPORT_SERVICE_TEMPLATES_BASE_PATH:reports/templates}
report.service.default-file-name=${REPORT_SERVICE_DEFAULT_FILE_NAME:report}
```

## Usage

```java
ReportResponse response = reportService.renderReport(
        ReportRequest.builder()
                .reportName("Daily Sales")
                .templatePath("daily_sales.jrxml")
                .format(ReportFormat.PDF)
                .parameters(Map.of("REPORT_TITLE", "Daily Sales"))
                .rows(rows)
                .build()
);
```

`templatePath` may be:

- relative to `classpath:reports/templates`
- an explicit `classpath:` resource
- a `file:` resource

## Supported Formats

- `PDF`
- `XLSX`
- `CSV`
