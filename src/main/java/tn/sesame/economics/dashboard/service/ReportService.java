package tn.sesame.economics.dashboard.service;

import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.ProductType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Comprehensive Report Generation Service with LLM integration
 */
public class ReportService {

    private ChatLanguageModel llmModel;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ReportTemplate> reportTemplates;
    private final List<ReportDTO> reportHistory;  // Changed to ReportDTO
    private boolean useLocalLLM;

    // Internal template class
    private static class ReportTemplate {
        private final String name;
        private final String content;
        private final Map<String, String> variables;

        public ReportTemplate(String name, String content) {
            this.name = name;
            this.content = content;
            this.variables = new HashMap<>();
        }

        public void addVariable(String key, String defaultValue) {
            variables.put(key, defaultValue);
        }

        public String render(Map<String, String> values) {
            String rendered = content;
            for (Map.Entry<String, String> var : variables.entrySet()) {
                String key = "{{" + var.getKey() + "}}";
                String value = values.getOrDefault(var.getKey(), var.getValue());
                rendered = rendered.replace(key, value);
            }
            return rendered;
        }

        public String getName() { return name; }
    }

    public ReportService(boolean useLocalLLM) {
        this.useLocalLLM = useLocalLLM;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.reportTemplates = new HashMap<>();
        this.reportHistory = new ArrayList<>();

        initializeLLM();
        initializeTemplates();
        createReportsDirectory();
    }

    private void initializeLLM() {
        try {
            if (useLocalLLM) {
                // Try TinyLlama first (small, efficient local model)
                try {
                    llmModel = OllamaChatModel.builder()
                            .baseUrl("http://localhost:11434")
                            .modelName("tinyllama")
                            .timeout(java.time.Duration.ofSeconds(120))
                            .build();
                    System.out.println("✅ Connected to TinyLlama (local)");
                } catch (Exception e) {
                    System.out.println("⚠️ TinyLlama not available, trying Llama 2");
                    llmModel = OllamaChatModel.builder()
                            .baseUrl("http://localhost:11434")
                            .modelName("llama2")
                            .timeout(java.time.Duration.ofSeconds(120))
                            .build();
                }
            } else {
                // Use OpenAI (requires API key)
                String apiKey = System.getenv("OPENAI_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    throw new IllegalStateException("OpenAI API key not found in environment variables");
                }
                llmModel = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-3.5-turbo")
                        .timeout(java.time.Duration.ofSeconds(30))
                        .build();
                System.out.println("✅ Connected to OpenAI");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize LLM: " + e.getMessage());
            System.out.println("⚠️ Continuing without LLM for report generation");
            llmModel = null;
        }
    }

    private void initializeTemplates() {
        // Market Intelligence Report Template
        ReportTemplate marketReport = new ReportTemplate(
                "market_intelligence",
                """
                # Market Intelligence Report
                ## {{report_title}}
                
                **Generated:** {{generation_date}}
                **Period:** {{period}}
                **Products Analyzed:** {{products_count}}
                
                ### Executive Summary
                {{executive_summary}}
                
                ### Price Analysis
                {{price_analysis}}
                
                ### Market Trends
                {{market_trends}}
                
                ### Risk Assessment
                {{risk_assessment}}
                
                ### Strategic Recommendations
                {{recommendations}}
                
                ### Data Summary
                {{data_summary}}
                
                ---
                *This report was automatically generated by the Tunisian Economic Intelligence System.*
                """
        );
        marketReport.addVariable("report_title", "Tunisian Agricultural Export Analysis");
        marketReport.addVariable("period", "Last 30 days");
        marketReport.addVariable("products_count", "All products");
        reportTemplates.put("market_intelligence", marketReport);

        // Predictive Analytics Report Template
        ReportTemplate predictiveReport = new ReportTemplate(
                "predictive_analytics",
                """
                # Predictive Analytics Report
                ## {{report_title}}
                
                **Model:** {{model_name}}
                **Confidence Threshold:** {{confidence_threshold}}
                **Horizon:** {{prediction_horizon}}
                
                ### Model Performance
                {{model_performance}}
                
                ### Key Predictions
                {{key_predictions}}
                
                ### Confidence Analysis
                {{confidence_analysis}}
                
                ### What-if Scenarios
                {{scenario_analysis}}
                
                ### Actionable Insights
                {{actionable_insights}}
                """
        );
        predictiveReport.addVariable("model_name", "DJL Price Predictor");
        predictiveReport.addVariable("confidence_threshold", "70%");
        predictiveReport.addVariable("prediction_horizon", "30 days");
        reportTemplates.put("predictive_analytics", predictiveReport);

        // Executive Summary Template
        ReportTemplate executiveReport = new ReportTemplate(
                "executive_summary",
                """
                # Executive Summary
                ## {{company_name}} - Agricultural Export Intelligence
                
                ### Key Metrics
                - **Total Predictions:** {{total_predictions}}
                - **Average Confidence:** {{avg_confidence}}
                - **High Risk Predictions:** {{high_risk_count}}
                - **Recommended Actions:** {{action_count}}
                
                ### Top Opportunities
                {{top_opportunities}}
                
                ### Critical Risks
                {{critical_risks}}
                
                ### Financial Impact
                {{financial_impact}}
                
                ### Next Steps
                {{next_steps}}
                """
        );
        executiveReport.addVariable("company_name", "Tunisian Agricultural Exports");
        reportTemplates.put("executive_summary", executiveReport);
    }

    private void createReportsDirectory() {
        try {
            Files.createDirectories(Paths.get("reports"));
            Files.createDirectories(Paths.get("reports/pdf"));
            Files.createDirectories(Paths.get("reports/html"));
            Files.createDirectories(Paths.get("reports/markdown"));
            System.out.println("✅ Created reports directory structure");
        } catch (IOException e) {
            System.err.println("❌ Failed to create reports directory: " + e.getMessage());
        }
    }

    /**
     * Generate a comprehensive market intelligence report using LLM
     */
    public String generateMarketIntelligenceReport(List<PricePrediction> predictions,
                                                   List<ExportData> historicalData,
                                                   Map<String, String> customVariables) {

        if (predictions == null || predictions.isEmpty()) {
            return "❌ No predictions available for report generation";
        }

        try {
            // Prepare data for LLM
            String dataSummary = prepareDataSummary(predictions, historicalData);

            // Generate report sections using LLM
            Map<String, String> reportSections = new HashMap<>();

            if (llmModel != null) {
                reportSections.put("executive_summary",
                        generateLLMSection("executive_summary", dataSummary));
                reportSections.put("price_analysis",
                        generateLLMSection("price_analysis", dataSummary));
                reportSections.put("market_trends",
                        generateLLMSection("market_trends", dataSummary));
                reportSections.put("risk_assessment",
                        generateLLMSection("risk_assessment", dataSummary));
                reportSections.put("recommendations",
                        generateLLMSection("recommendations", dataSummary));
            } else {
                // Fallback to template-based generation
                reportSections.put("executive_summary", generateExecutiveSummary(predictions));
                reportSections.put("price_analysis", generatePriceAnalysis(predictions));
                reportSections.put("market_trends", generateMarketTrends(historicalData));
                reportSections.put("risk_assessment", generateRiskAssessment(predictions));
                reportSections.put("recommendations", generateRecommendations(predictions));
            }

            // Prepare template variables
            Map<String, String> templateVariables = new HashMap<>();
            templateVariables.put("generation_date", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            templateVariables.put("data_summary", dataSummary);
            templateVariables.putAll(reportSections);
            if (customVariables != null) {
                templateVariables.putAll(customVariables);
            }

            // Get template
            ReportTemplate template = reportTemplates.get("market_intelligence");
            String reportContent = template.render(templateVariables);

            // Generate report ID
            String reportId = "MI_" + System.currentTimeMillis();
            String reportName = "Market_Intelligence_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // Save to history as ReportDTO
            ReportDTO report = new ReportDTO(
                    reportId,
                    reportName,
                    LocalDateTime.now(),
                    "MARKDOWN",
                    "reports/markdown/" + reportName + ".md",
                    1
            );
            reportHistory.add(report);

            System.out.println("✅ Generated market intelligence report: " + reportId);
            return reportContent;

        } catch (Exception e) {
            return "❌ Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generate a section using LLM
     */
    private String generateLLMSection(String sectionType, String dataSummary) {
        if (llmModel == null) {
            return "LLM not available for content generation.";
        }

        String prompt = switch (sectionType) {
            case "executive_summary" ->
                    "Based on the following Tunisian agricultural export data, write a concise executive summary (3-4 paragraphs):\n\n" + dataSummary;
            case "price_analysis" ->
                    "Analyze the price trends in this Tunisian agricultural export data. Focus on patterns, anomalies, and significant changes:\n\n" + dataSummary;
            case "market_trends" ->
                    "Identify market trends from this Tunisian agricultural export data. Consider seasonal patterns, demand changes, and market indicators:\n\n" + dataSummary;
            case "risk_assessment" ->
                    "Assess risks based on this Tunisian agricultural export data. Consider price volatility, confidence levels, and market conditions:\n\n" + dataSummary;
            case "recommendations" ->
                    "Provide strategic recommendations for Tunisian agricultural exporters based on this data. Be specific and actionable:\n\n" + dataSummary;
            default -> "Analyze this data:\n\n" + dataSummary;
        };

        try {
            return llmModel.generate(prompt);
        } catch (Exception e) {
            System.err.println("❌ LLM generation failed for " + sectionType + ": " + e.getMessage());
            return "Content generation failed. Please try again or use template mode.";
        }
    }

    /**
     * Export report in multiple formats
     */
    public Map<String, String> exportReport(String reportContent, String reportName, String[] formats) {
        Map<String, String> results = new HashMap<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        for (String format : formats) {
            try {
                String filePath = switch (format.toUpperCase()) {
                    case "PDF" -> exportToPDF(reportContent, reportName, timestamp);
                    case "HTML" -> exportToHTML(reportContent, reportName, timestamp);
                    case "MARKDOWN" -> exportToMarkdown(reportContent, reportName, timestamp);
                    default -> throw new IllegalArgumentException("Unsupported format: " + format);
                };

                results.put(format, "✅ Exported to: " + filePath);

            } catch (Exception e) {
                results.put(format, "❌ Export failed: " + e.getMessage());
            }
        }

        return results;
    }

    private String exportToPDF(String content, String reportName, String timestamp) throws IOException {
        String fileName = "reports/pdf/" + reportName + "_" + timestamp + ".pdf";

        try (PdfWriter writer = new PdfWriter(fileName);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Add title
            document.add(new Paragraph("Tunisian Economic Intelligence System")
                    .setFontSize(20)
                    .setBold());

            document.add(new Paragraph(reportName)
                    .setFontSize(16)
                    .setItalic());

            document.add(new Paragraph("Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFontSize(10));

            document.add(new Paragraph("\n"));

            // Convert markdown content to PDF paragraphs
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    document.add(new Paragraph(line.substring(2))
                            .setFontSize(16)
                            .setBold());
                } else if (line.startsWith("## ")) {
                    document.add(new Paragraph(line.substring(3))
                            .setFontSize(14)
                            .setBold());
                } else if (line.startsWith("### ")) {
                    document.add(new Paragraph(line.substring(4))
                            .setFontSize(12)
                            .setBold());
                } else if (!line.trim().isEmpty()) {
                    document.add(new Paragraph(line));
                }
            }

            System.out.println("✅ PDF exported: " + fileName);
            return fileName;
        }
    }

    private String exportToHTML(String content, String reportName, String timestamp) throws IOException {
        String fileName = "reports/html/" + reportName + "_" + timestamp + ".html";

        // Simple HTML template
        String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
                    h1 { color: #2c3e50; border-bottom: 2px solid #3498db; }
                    h2 { color: #34495e; margin-top: 30px; }
                    h3 { color: #7f8c8d; }
                    .header { background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 30px; }
                    .timestamp { color: #95a5a6; font-size: 0.9em; }
                    .content { margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Tunisian Economic Intelligence System</h1>
                    <h2>%s</h2>
                    <p class="timestamp">Generated: %s</p>
                </div>
                <div class="content">
                    %s
                </div>
            </body>
            </html>
            """;

        // Convert markdown to simple HTML
        String htmlContent = content
                .replace("# ", "<h1>").replace("\n# ", "</h1>\n<h1>")
                .replace("## ", "<h2>").replace("\n## ", "</h2>\n<h2>")
                .replace("### ", "<h3>").replace("\n### ", "</h3>\n<h3>")
                .replace("\n\n", "</p><p>")
                .replace("\n", "<br>");

        String fullHtml = String.format(htmlTemplate,
                reportName,
                reportName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                htmlContent);

        Files.write(Paths.get(fileName), fullHtml.getBytes());
        System.out.println("✅ HTML exported: " + fileName);
        return fileName;
    }

    private String exportToMarkdown(String content, String reportName, String timestamp) throws IOException {
        String fileName = "reports/markdown/" + reportName + "_" + timestamp + ".md";

        // Add header to markdown
        String header = String.format(
                "---\n" +
                        "title: %s\n" +
                        "generated: %s\n" +
                        "system: Tunisian Economic Intelligence System\n" +
                        "---\n\n",
                reportName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        Files.write(Paths.get(fileName), (header + content).getBytes());
        System.out.println("✅ Markdown exported: " + fileName);
        return fileName;
    }

    /**
     * Schedule automated report generation
     */
    public void scheduleReport(String reportType, String cronExpression, Map<String, String> parameters) {
        System.out.println("📅 Scheduled " + reportType + " report with cron: " + cronExpression);

        // For demo, we'll just schedule a daily report
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("⏰ Executing scheduled report: " + reportType);
                // In a real implementation, this would generate the actual report
                System.out.println("✅ Scheduled report completed at: " + LocalDateTime.now());
            } catch (Exception e) {
                System.err.println("❌ Scheduled report failed: " + e.getMessage());
            }
        }, 0, 24, TimeUnit.HOURS); // Run daily
    }

    /**
     * Get report history - returns List<ReportDTO>
     */
    public List<ReportDTO> getReportHistory() {
        return new ArrayList<>(reportHistory);
    }

    /**
     * Get a specific report by ID - returns Optional<ReportDTO>
     */
    public Optional<ReportDTO> getReportById(String reportId) {
        return reportHistory.stream()
                .filter(r -> r.getReportId().equals(reportId))
                .findFirst();
    }

    /**
     * Create a new report version
     */
    public String createReportVersion(String originalReportId, String updatedContent) {
        Optional<ReportDTO> original = getReportById(originalReportId);
        if (original.isEmpty()) {
            return "❌ Original report not found";
        }

        ReportDTO originalReport = original.get();
        int newVersion = originalReport.getVersion() + 1;
        String newReportId = originalReport.getReportId() + "_v" + newVersion;

        ReportDTO newVersionReport = new ReportDTO(
                newReportId,
                originalReport.getReportName() + " (v" + newVersion + ")",
                LocalDateTime.now(),
                originalReport.getFormat(),
                originalReport.getFilePath().replace(".", "_v" + newVersion + "."),
                newVersion
        );

        reportHistory.add(newVersionReport);
        return "✅ Created version " + newVersion + " of report " + originalReportId;
    }

    // Helper methods for template generation (when LLM is not available)
    private String prepareDataSummary(List<PricePrediction> predictions, List<ExportData> historicalData) {
        StringBuilder summary = new StringBuilder();

        summary.append("=== DATA SUMMARY ===\n\n");
        summary.append("Total Predictions: ").append(predictions.size()).append("\n");

        // Product distribution
        Map<ProductType, Long> productCount = predictions.stream()
                .collect(Collectors.groupingBy(PricePrediction::productType, Collectors.counting()));

        summary.append("\nProduct Distribution:\n");
        productCount.forEach((product, count) ->
                summary.append("  - ").append(product.getFrenchName())
                        .append(": ").append(count).append(" predictions\n"));

        // Confidence levels
        long highConfidence = predictions.stream()
                .filter(p -> p.confidence() >= 0.8)
                .count();
        long mediumConfidence = predictions.stream()
                .filter(p -> p.confidence() >= 0.6 && p.confidence() < 0.8)
                .count();
        long lowConfidence = predictions.stream()
                .filter(p -> p.confidence() < 0.6)
                .count();

        summary.append("\nConfidence Levels:\n");
        summary.append("  - High (≥80%): ").append(highConfidence).append("\n");
        summary.append("  - Medium (60-80%): ").append(mediumConfidence).append("\n");
        summary.append("  - Low (<60%): ").append(lowConfidence).append("\n");

        // Price statistics
        DoubleSummaryStatistics priceStats = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .summaryStatistics();

        summary.append("\nPrice Statistics (TND/ton):\n");
        summary.append("  - Average: ").append(String.format("%.2f", priceStats.getAverage())).append("\n");
        summary.append("  - Minimum: ").append(String.format("%.2f", priceStats.getMin())).append("\n");
        summary.append("  - Maximum: ").append(String.format("%.2f", priceStats.getMax())).append("\n");
        summary.append("  - Range: ").append(String.format("%.2f", priceStats.getMax() - priceStats.getMin())).append("\n");

        if (historicalData != null && !historicalData.isEmpty()) {
            summary.append("\nHistorical Data: ").append(historicalData.size()).append(" records\n");
        }

        return summary.toString();
    }

    private String generateExecutiveSummary(List<PricePrediction> predictions) {
        double avgConfidence = predictions.stream()
                .mapToDouble(PricePrediction::confidence)
                .average()
                .orElse(0);

        double avgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0);

        return String.format(
                "This report analyzes %d price predictions with an average confidence of %.1f%%. " +
                        "The average predicted price is %.2f TND/ton across all agricultural products. " +
                        "Key opportunities and risks have been identified based on confidence levels and market indicators.",
                predictions.size(), avgConfidence * 100, avgPrice
        );
    }

    private String generatePriceAnalysis(List<PricePrediction> predictions) {
        Map<ProductType, Double> avgPriceByProduct = predictions.stream()
                .collect(Collectors.groupingBy(
                        PricePrediction::productType,
                        Collectors.averagingDouble(PricePrediction::predictedPrice)
                ));

        StringBuilder analysis = new StringBuilder("### Price Analysis by Product\n\n");
        avgPriceByProduct.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(entry ->
                        analysis.append(String.format("- **%s**: %.2f TND/ton\n",
                                entry.getKey().getFrenchName(), entry.getValue())));

        return analysis.toString();
    }

    private String generateMarketTrends(List<ExportData> historicalData) {
        if (historicalData == null || historicalData.isEmpty()) {
            return "Insufficient historical data for trend analysis.";
        }

        return "Market trends analysis requires historical data patterns. " +
                "Consider seasonal variations, export volumes, and destination market changes " +
                "for comprehensive trend identification.";
    }

    private String generateRiskAssessment(List<PricePrediction> predictions) {
        long highRisk = predictions.stream()
                .filter(p -> p.confidence() < 0.5)
                .count();

        long lowConfidence = predictions.stream()
                .filter(p -> p.confidence() < 0.6)
                .count();

        return String.format(
                "### Risk Assessment\n\n" +
                        "- **High Risk Predictions**: %d (%.1f%%)\n" +
                        "- **Low Confidence Predictions**: %d (%.1f%%)\n\n" +
                        "Recommend close monitoring of high-risk predictions and verification of low-confidence results.",
                highRisk, (double) highRisk / predictions.size() * 100,
                lowConfidence, (double) lowConfidence / predictions.size() * 100
        );
    }

    private String generateRecommendations(List<PricePrediction> predictions) {
        // Find top products by average price
        Map<ProductType, Double> avgPriceByProduct = predictions.stream()
                .collect(Collectors.groupingBy(
                        PricePrediction::productType,
                        Collectors.averagingDouble(PricePrediction::predictedPrice)
                ));

        Optional<Map.Entry<ProductType, Double>> topProduct = avgPriceByProduct.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        StringBuilder recommendations = new StringBuilder("### Strategic Recommendations\n\n");

        recommendations.append("1. **Focus on High-Value Products**: ");
        if (topProduct.isPresent()) {
            recommendations.append(String.format("Prioritize %s with average predicted price of %.2f TND/ton.\n",
                    topProduct.get().getKey().getFrenchName(), topProduct.get().getValue()));
        }

        recommendations.append("2. **Monitor High-Risk Predictions**: Review predictions with confidence below 60%.\n");
        recommendations.append("3. **Diversify Export Markets**: Consider alternative destinations for risk mitigation.\n");
        recommendations.append("4. **Optimize Export Timing**: Use predictions to identify optimal export periods.\n");

        return recommendations.toString();
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("✅ Report service shutdown complete");
    }
}