package tn.sesame.economics.dashboard.service;

import java.time.LocalDateTime;

/**
 * Public DTO for generated reports that can be accessed from outside
 */
public class ReportDTO {
    private final String reportId;
    private final String reportName;
    private final LocalDateTime generationTime;
    private final String format;
    private final String filePath;
    private final int version;

    public ReportDTO(String reportId, String reportName, LocalDateTime generationTime,
                     String format, String filePath, int version) {
        this.reportId = reportId;
        this.reportName = reportName;
        this.generationTime = generationTime;
        this.format = format;
        this.filePath = filePath;
        this.version = version;
    }

    // Getters
    public String getReportId() { return reportId; }
    public String getReportName() { return reportName; }
    public LocalDateTime getGenerationTime() { return generationTime; }
    public String getFormat() { return format; }
    public String getFilePath() { return filePath; }
    public int getVersion() { return version; }
}