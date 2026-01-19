package tn.sesame.economics.dashboard.view;

import tn.sesame.economics.dashboard.service.ReportService;
import tn.sesame.economics.dashboard.service.ReportDTO;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ExportData;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Report Generation Dashboard Component
 */
public class ReportGenerationDashboard extends VBox {

    private final ReportService reportService;
    private List<PricePrediction> currentPredictions;
    private List<ExportData> historicalData;
    private String lastGeneratedReportContent;
    private String lastGeneratedReportName;

    // UI Components
    private ComboBox<String> reportTypeCombo;
    private ComboBox<String> templateCombo;
    private CheckBox useLLMCheckbox;
    private CheckBox scheduleReportCheckbox;
    private ComboBox<String> formatCombo;
    private ComboBox<String> scheduleCombo;
    private TextArea customVariablesArea;
    private TextArea reportPreviewArea;
    private Button generateButton;
    private Button exportButton;
    private Button scheduleButton;
    private Button savePreviewButton;
    private Button createVersionButton;
    private Button refreshPreviewButton;
    private Button refreshHistoryButton;
    private Button clearHistoryButton;
    private ProgressBar generationProgress;
    private Label statusLabel;

    // Report history
    private TableView<ReportHistoryItem> historyTable;
    private ObservableList<ReportHistoryItem> reportHistory;

    public ReportGenerationDashboard(ReportService reportService) {
        this.reportService = reportService;
        this.reportHistory = FXCollections.observableArrayList();
        this.lastGeneratedReportContent = "";
        this.lastGeneratedReportName = "";

        initializeUI();
        loadReportHistory();
    }

    public void setData(List<PricePrediction> predictions, List<ExportData> historicalData) {
        this.currentPredictions = predictions;
        this.historicalData = historicalData;
        updateStatus("Data loaded: " + predictions.size() + " predictions");

        // Auto-populate custom variables with data summary
        if (predictions != null && !predictions.isEmpty()) {
            Map<String, String> variables = new HashMap<>();
            variables.put("total_predictions", String.valueOf(predictions.size()));
            variables.put("report_title", "Market Intelligence - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            variables.put("period", "Last 30 Days");

            // Convert to JSON-like string
            StringBuilder jsonBuilder = new StringBuilder("{\n");
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                jsonBuilder.append("  \"").append(entry.getKey()).append("\": \"")
                        .append(entry.getValue()).append("\",\n");
            }
            if (variables.size() > 0) {
                jsonBuilder.delete(jsonBuilder.length() - 2, jsonBuilder.length());
            }
            jsonBuilder.append("\n}");

            customVariablesArea.setText(jsonBuilder.toString());
        }
    }

    private void initializeUI() {
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        // Title
        Label title = new Label("📊 REPORT GENERATION SYSTEM");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Main content with two columns
        HBox mainContent = new HBox(20);

        // Left: Configuration Panel
        VBox configPanel = createConfigurationPanel();
        configPanel.setPrefWidth(400);

        // Right: Preview and History Panel
        VBox previewPanel = createPreviewPanel();
        previewPanel.setPrefWidth(600);

        mainContent.getChildren().addAll(configPanel, previewPanel);

        getChildren().addAll(title, mainContent);
    }

    private VBox createConfigurationPanel() {
        VBox configPanel = new VBox(15);
        configPanel.setPadding(new Insets(20));
        configPanel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");

        Label configTitle = new Label("⚙️ REPORT CONFIGURATION");
        configTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Report Type
        Label typeLabel = new Label("Report Type:");
        reportTypeCombo = new ComboBox<>();
        reportTypeCombo.getItems().addAll(
                "Market Intelligence",
                "Predictive Analytics",
                "Executive Summary",
                "Custom Report"
        );
        reportTypeCombo.setValue("Market Intelligence");

        // Template Selection
        Label templateLabel = new Label("Template:");
        templateCombo = new ComboBox<>();
        templateCombo.getItems().addAll(
                "Standard Template",
                "Detailed Analysis",
                "Executive Brief",
                "Custom Template"
        );
        templateCombo.setValue("Standard Template");

        // LLM Options
        HBox llmBox = new HBox(10);
        useLLMCheckbox = new CheckBox("Use AI (TinyLlama/OpenAI)");
        useLLMCheckbox.setSelected(true);
        Label llmStatus = new Label("🟢 LLM Available");
        llmStatus.setTextFill(Color.GREEN);
        llmBox.getChildren().addAll(useLLMCheckbox, llmStatus);

        // Export Formats
        Label formatLabel = new Label("Export Format(s):");
        formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(
                "PDF only",
                "HTML only",
                "Markdown only",
                "All Formats (PDF+HTML+MD)"
        );
        formatCombo.setValue("All Formats (PDF+HTML+MD)");

        // Scheduling
        HBox scheduleBox = new HBox(10);
        scheduleReportCheckbox = new CheckBox("Schedule Report");
        scheduleCombo = new ComboBox<>();
        scheduleCombo.getItems().addAll(
                "Daily",
                "Weekly",
                "Monthly",
                "Custom"
        );
        scheduleCombo.setValue("Daily");
        scheduleCombo.setDisable(true);
        scheduleReportCheckbox.selectedProperty().addListener((obs, oldVal, newVal) ->
                scheduleCombo.setDisable(!newVal));
        scheduleBox.getChildren().addAll(scheduleReportCheckbox, scheduleCombo);

        // Custom Variables
        Label variablesLabel = new Label("Custom Variables (JSON):");
        customVariablesArea = new TextArea();
        customVariablesArea.setPromptText("{\n  \"report_title\": \"Custom Title\",\n  \"period\": \"Q1 2024\"\n}");
        customVariablesArea.setPrefHeight(100);
        customVariablesArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");

        // Action Buttons
        HBox buttonBox = new HBox(10);
        generateButton = createActionButton("🤖 Generate Report", "#4CAF50");
        exportButton = createActionButton("📥 Export Report", "#2196F3");
        scheduleButton = createActionButton("⏰ Schedule", "#FF9800");

        exportButton.setDisable(true);

        generateButton.setOnAction(e -> generateReport());
        exportButton.setOnAction(e -> exportReport());
        scheduleButton.setOnAction(e -> scheduleReport());

        buttonBox.getChildren().addAll(generateButton, exportButton, scheduleButton);

        // Progress
        generationProgress = new ProgressBar(0);
        generationProgress.setVisible(false);

        statusLabel = new Label("Ready to generate reports");
        statusLabel.setStyle("-fx-font-size: 12px;");

        configPanel.getChildren().addAll(
                configTitle,
                typeLabel, reportTypeCombo,
                templateLabel, templateCombo,
                llmBox,
                formatLabel, formatCombo,
                scheduleBox,
                variablesLabel, customVariablesArea,
                buttonBox,
                generationProgress,
                statusLabel
        );

        return configPanel;
    }

    private VBox createPreviewPanel() {
        VBox previewPanel = new VBox(15);
        previewPanel.setPadding(new Insets(20));
        previewPanel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");

        // Report Preview
        Label previewTitle = new Label("👁️ REPORT PREVIEW");
        previewTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        reportPreviewArea = new TextArea();
        reportPreviewArea.setPromptText("Generated report will appear here...");
        reportPreviewArea.setPrefHeight(250);
        reportPreviewArea.setEditable(true);
        reportPreviewArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");

        // Action buttons for preview
        HBox previewButtons = new HBox(10);
        savePreviewButton = createActionButton("💾 Save Changes", "#4CAF50");
        createVersionButton = createActionButton("🔄 Create New Version", "#9C27B0");
        refreshPreviewButton = createActionButton("🔄 Refresh", "#607D8B");

        savePreviewButton.setOnAction(e -> savePreviewChanges());
        createVersionButton.setOnAction(e -> createNewVersion());
        refreshPreviewButton.setOnAction(e -> refreshPreview());

        previewButtons.getChildren().addAll(savePreviewButton, createVersionButton, refreshPreviewButton);

        // Report History
        Label historyTitle = new Label("📜 REPORT HISTORY");
        historyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        historyTable = new TableView<>();
        historyTable.setPrefHeight(200);

        // Setup table columns
        TableColumn<ReportHistoryItem, String> nameCol = new TableColumn<>("Report Name");
        nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        nameCol.setPrefWidth(200);

        TableColumn<ReportHistoryItem, String> dateCol = new TableColumn<>("Generated");
        dateCol.setCellValueFactory(cell -> cell.getValue().dateProperty());
        dateCol.setPrefWidth(150);

        TableColumn<ReportHistoryItem, String> formatCol = new TableColumn<>("Format");
        formatCol.setCellValueFactory(cell -> cell.getValue().formatProperty());
        formatCol.setPrefWidth(100);

        TableColumn<ReportHistoryItem, String> versionCol = new TableColumn<>("Version");
        versionCol.setCellValueFactory(cell -> cell.getValue().versionProperty());
        versionCol.setPrefWidth(80);

        TableColumn<ReportHistoryItem, String> actionCol = new TableColumn<>("Actions");
        actionCol.setCellValueFactory(cell -> cell.getValue().actionProperty());
        actionCol.setPrefWidth(100);
        actionCol.setCellFactory(col -> new TableCell<ReportHistoryItem, String>() {
            final Button viewButton = new Button("View");
            final Button exportButton = new Button("Export");

            {
                viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 5 10;");
                exportButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 10;");

                viewButton.setOnAction(e -> {
                    ReportHistoryItem item = getTableView().getItems().get(getIndex());
                    viewReport(item);
                });

                exportButton.setOnAction(e -> {
                    ReportHistoryItem item = getTableView().getItems().get(getIndex());
                    exportSingleReport(item);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.getChildren().addAll(viewButton, exportButton);
                    setGraphic(buttons);
                }
            }
        });

        historyTable.getColumns().addAll(nameCol, dateCol, formatCol, versionCol, actionCol);
        historyTable.setItems(reportHistory);

        // History action buttons
        HBox historyButtons = new HBox(10);
        refreshHistoryButton = createActionButton("🔄 Refresh History", "#607D8B");
        clearHistoryButton = createActionButton("🗑️ Clear History", "#F44336");

        refreshHistoryButton.setOnAction(e -> loadReportHistory());
        clearHistoryButton.setOnAction(e -> clearHistory());

        historyButtons.getChildren().addAll(refreshHistoryButton, clearHistoryButton);

        previewPanel.getChildren().addAll(
                previewTitle,
                reportPreviewArea,
                previewButtons,
                historyTitle,
                historyTable,
                historyButtons
        );

        return previewPanel;
    }

    private void generateReport() {
        if (currentPredictions == null || currentPredictions.isEmpty()) {
            showAlert("No Data", "Please load prediction data before generating reports.", Alert.AlertType.WARNING);
            return;
        }

        try {
            updateStatus("Generating report...");
            generationProgress.setVisible(true);
            generationProgress.setProgress(-1); // Indeterminate

            // Parse custom variables
            Map<String, String> customVariables = parseCustomVariables();

            // Generate report based on type
            String reportType = reportTypeCombo.getValue();
            String reportContent = "";

            if ("Market Intelligence".equals(reportType)) {
                reportContent = reportService.generateMarketIntelligenceReport(
                        currentPredictions, historicalData, customVariables);
            } else if ("Predictive Analytics".equals(reportType)) {
                // In a full implementation, you would have a separate method for this
                reportContent = "Predictive Analytics Report\n\n" +
                        "This report type requires additional implementation.\n" +
                        "Generated from " + currentPredictions.size() + " predictions.";
            } else if ("Executive Summary".equals(reportType)) {
                reportContent = "Executive Summary Report\n\n" +
                        "Summary of " + currentPredictions.size() + " predictions.\n" +
                        "Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                reportContent = "Custom Report\n\n" +
                        "Generated with custom variables: " + customVariables.toString();
            }

            // Update preview
            reportPreviewArea.setText(reportContent);
            lastGeneratedReportContent = reportContent;
            lastGeneratedReportName = reportType + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // Enable export button
            exportButton.setDisable(false);

            // Update status
            updateStatus("✅ Report generated successfully!");

            // Refresh history
            loadReportHistory();

        } catch (Exception e) {
            showAlert("Generation Error", "Failed to generate report: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatus("❌ Report generation failed: " + e.getMessage());
        } finally {
            generationProgress.setVisible(false);
        }
    }

    private void exportReport() {
        if (lastGeneratedReportContent.isEmpty()) {
            showAlert("No Report", "Please generate a report before exporting.", Alert.AlertType.WARNING);
            return;
        }

        try {
            updateStatus("Exporting report...");

            // Determine formats based on selection
            String formatSelection = formatCombo.getValue();
            String[] formats = new String[0];

            if (formatSelection.contains("All Formats")) {
                formats = new String[]{"PDF", "HTML", "MARKDOWN"};
            } else if (formatSelection.contains("PDF")) {
                formats = new String[]{"PDF"};
            } else if (formatSelection.contains("HTML")) {
                formats = new String[]{"HTML"};
            } else if (formatSelection.contains("Markdown")) {
                formats = new String[]{"MARKDOWN"};
            }

            // Export the report
            Map<String, String> exportResults = reportService.exportReport(
                    lastGeneratedReportContent,
                    lastGeneratedReportName,
                    formats
            );

            // Show results
            StringBuilder resultMessage = new StringBuilder("Export Results:\n");
            for (Map.Entry<String, String> result : exportResults.entrySet()) {
                resultMessage.append("- ").append(result.getKey()).append(": ").append(result.getValue()).append("\n");
            }

            showAlert("Export Complete", resultMessage.toString(), Alert.AlertType.INFORMATION);
            updateStatus("✅ Report exported successfully!");

            // Refresh history
            loadReportHistory();

        } catch (Exception e) {
            showAlert("Export Error", "Failed to export report: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatus("❌ Export failed: " + e.getMessage());
        }
    }

    private void scheduleReport() {
        if (!scheduleReportCheckbox.isSelected()) {
            showAlert("Scheduling Disabled", "Please enable scheduling first.", Alert.AlertType.WARNING);
            return;
        }

        try {
            String scheduleType = scheduleCombo.getValue();
            Map<String, String> parameters = new HashMap<>();
            parameters.put("report_type", reportTypeCombo.getValue());
            parameters.put("template", templateCombo.getValue());

            // Parse custom variables if provided
            Map<String, String> customVars = parseCustomVariables();
            parameters.putAll(customVars);

            // Create cron expression based on schedule
            String cronExpression = "";
            switch (scheduleType) {
                case "Daily":
                    cronExpression = "0 0 9 * * ?"; // 9 AM daily
                    break;
                case "Weekly":
                    cronExpression = "0 0 9 ? * MON"; // 9 AM every Monday
                    break;
                case "Monthly":
                    cronExpression = "0 0 9 1 * ?"; // 9 AM on 1st day of month
                    break;
                default:
                    cronExpression = "0 0 9 * * ?"; // Default to daily
            }

            // Schedule the report
            reportService.scheduleReport(reportTypeCombo.getValue(), cronExpression, parameters);

            showAlert("Scheduling Complete",
                    "Report scheduled for " + scheduleType.toLowerCase() + " generation.\n" +
                            "Cron expression: " + cronExpression,
                    Alert.AlertType.INFORMATION);

            updateStatus("⏰ Report scheduled for " + scheduleType.toLowerCase() + " generation");

        } catch (Exception e) {
            showAlert("Scheduling Error", "Failed to schedule report: " + e.getMessage(), Alert.AlertType.ERROR);
            updateStatus("❌ Scheduling failed: " + e.getMessage());
        }
    }

    private Map<String, String> parseCustomVariables() {
        Map<String, String> variables = new HashMap<>();

        try {
            String jsonText = customVariablesArea.getText().trim();
            if (!jsonText.isEmpty() && jsonText.startsWith("{") && jsonText.endsWith("}")) {
                // Simple JSON parsing (for demo - in production use a JSON library)
                jsonText = jsonText.substring(1, jsonText.length() - 1).trim();
                String[] pairs = jsonText.split(",");

                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "").trim();
                        String value = keyValue[1].trim().replace("\"", "").trim();
                        if (!key.isEmpty() && !value.isEmpty()) {
                            variables.put(key, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse custom variables: " + e.getMessage());
        }

        // Add default variables if empty
        if (variables.isEmpty()) {
            variables.put("report_title", "Market Intelligence Report");
            variables.put("period", "Last 30 Days");
        }

        return variables;
    }

    private void loadReportHistory() {
        try {
            reportHistory.clear();

            List<ReportDTO> reports = reportService.getReportHistory();
            for (ReportDTO report : reports) {
                reportHistory.add(new ReportHistoryItem(
                        report.getReportName(),
                        report.getGenerationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        report.getFormat(),
                        String.valueOf(report.getVersion()),
                        report.getReportId()
                ));
            }

            updateStatus("Loaded " + reports.size() + " reports from history");

        } catch (Exception e) {
            System.err.println("Failed to load report history: " + e.getMessage());
        }
    }

    private void viewReport(ReportHistoryItem item) {
        Optional<ReportDTO> reportOpt = reportService.getReportById(item.getReportId());
        if (reportOpt.isPresent()) {
            ReportDTO report = reportOpt.get();
            showAlert("Report Details",
                    "Report: " + report.getReportName() + "\n" +
                            "Generated: " + report.getGenerationTime() + "\n" +
                            "Format: " + report.getFormat() + "\n" +
                            "Version: " + report.getVersion() + "\n" +
                            "File: " + report.getFilePath(),
                    Alert.AlertType.INFORMATION);
        }
    }

    private void exportSingleReport(ReportHistoryItem item) {
        Optional<ReportDTO> reportOpt = reportService.getReportById(item.getReportId());
        if (reportOpt.isPresent()) {
            showAlert("Export Report",
                    "Export functionality for individual reports would be implemented here.\n" +
                            "Report: " + item.getName(),
                    Alert.AlertType.INFORMATION);
        }
    }

    private void savePreviewChanges() {
        String modifiedContent = reportPreviewArea.getText();
        lastGeneratedReportContent = modifiedContent;
        updateStatus("✅ Changes saved to preview");
    }

    private void createNewVersion() {
        if (!lastGeneratedReportName.isEmpty() && !lastGeneratedReportContent.isEmpty()) {
            // Get the latest report from history to version
            if (!reportHistory.isEmpty()) {
                ReportHistoryItem latest = reportHistory.get(reportHistory.size() - 1);
                String result = reportService.createReportVersion(latest.getReportId(), lastGeneratedReportContent);

                showAlert("Version Created", result, Alert.AlertType.INFORMATION);
                updateStatus("✅ Created new report version");
                loadReportHistory();
            }
        }
    }

    private void refreshPreview() {
        // Just show a message for demo
        updateStatus("Preview refreshed");
    }

    private void clearHistory() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Clear History");
        confirmAlert.setHeaderText("Clear Report History");
        confirmAlert.setContentText("Are you sure you want to clear all report history? This action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            reportHistory.clear();
            updateStatus("✅ Report history cleared");
        }
    }

    private Button createActionButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        button.setPrefHeight(40);
        button.setMinWidth(120);
        return button;
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("[Status] " + message);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        if (reportService != null) {
            reportService.shutdown();
        }
    }

    /**
     * Data class for report history table
     */
    public static class ReportHistoryItem {
        private final SimpleStringProperty name;
        private final SimpleStringProperty date;
        private final SimpleStringProperty format;
        private final SimpleStringProperty version;
        private final SimpleStringProperty action;
        private final String reportId;

        public ReportHistoryItem(String name, String date, String format, String version, String reportId) {
            this.name = new SimpleStringProperty(name);
            this.date = new SimpleStringProperty(date);
            this.format = new SimpleStringProperty(format);
            this.version = new SimpleStringProperty(version);
            this.action = new SimpleStringProperty("View/Export");
            this.reportId = reportId;
        }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }

        public String getDate() { return date.get(); }
        public SimpleStringProperty dateProperty() { return date; }

        public String getFormat() { return format.get(); }
        public SimpleStringProperty formatProperty() { return format; }

        public String getVersion() { return version.get(); }
        public SimpleStringProperty versionProperty() { return version; }

        public String getAction() { return action.get(); }
        public SimpleStringProperty actionProperty() { return action; }

        public String getReportId() { return reportId; }
    }
}