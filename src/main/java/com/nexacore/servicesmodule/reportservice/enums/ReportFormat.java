package com.nexacore.servicesmodule.reportservice.enums;

public enum ReportFormat {
    PDF("application/pdf", "pdf"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    CSV("text/csv", "csv");

    private final String contentType;
    private final String fileExtension;

    ReportFormat(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
