package tn.sesame.economics.dashboard.controller;

import tn.sesame.economics.dashboard.service.ChartDataService;
import tn.sesame.economics.dashboard.view.chart.InteractiveChartPanel;
import tn.sesame.economics.dashboard.view.DashboardView;
import tn.sesame.economics.dashboard.view.FilterPanel;
import tn.sesame.economics.dashboard.service.StatisticsService;
import tn.sesame.economics.dashboard.service.DataService;
import tn.sesame.economics.dashboard.service.FilterService;
import tn.sesame.economics.dashboard.service.ExportService;
import tn.sesame.economics.dashboard.model.DashboardStatistics;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ExportData; // ADD THIS LINE
import tn.sesame.economics.service.EconomicIntelligenceService;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for the dashboard (MVC Pattern)
 */
public class DashboardController {

    private final DashboardView view;
    private final StatisticsService statisticsService;
    private final DataService dataService;
    private final FilterService filterService;
    private final ExportService exportService; // ADD THIS
    private List<PricePrediction> currentPredictions;
    private List<PricePrediction> filteredPredictions;
    private ChartDataService chartDataService;

    public DashboardController(Stage primaryStage, EconomicIntelligenceService intelligenceService) {
        this.statisticsService = new StatisticsService();
        this.dataService = new DataService(intelligenceService);
        this.filterService = new FilterService();
        this.exportService = new ExportService();
        this.chartDataService = new ChartDataService(); // ADD THIS
        this.view = new DashboardView(primaryStage, dataService);
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Refresh button handler
        view.getRefreshButton().setOnAction(event -> refreshData());

        // Back button handler
        view.getBackButton().setOnAction(event -> {
            view.close();
            System.out.println("Returning to main menu...");
        });

        // Filter button handlers
        FilterPanel filterPanel = view.getFilterPanel();

        // Apply filter button
        filterPanel.getApplyFilterButton().setOnAction(event -> applyFilters());

        // Reset filter button
        filterPanel.getResetFilterButton().setOnAction(event -> {
            filterPanel.resetFilters();
            resetFilters();
        });

        // Export buttons - UPDATED WITH REAL FUNCTIONALITY
        filterPanel.getExportCsvButton().setOnAction(event -> {
            String result = exportService.exportFilteredData(
                    filteredPredictions,
                    "CSV",
                    generateExportFileName("csv")
            );
            showExportNotification(result, "CSV");
        });

        filterPanel.getExportJsonButton().setOnAction(event -> {
            String result = exportService.exportFilteredData(
                    filteredPredictions,
                    "JSON",
                    generateExportFileName("json")
            );
            showExportNotification(result, "JSON");
        });

        // Preset buttons
        filterPanel.getSavePresetButton().setOnAction(event -> saveFilterPreset());
        filterPanel.getLoadPresetButton().setOnAction(event -> loadFilterPreset());
    }

    private void loadInitialData() {
        // Load all data
        currentPredictions = dataService.generateRealPredictions();
        filteredPredictions = currentPredictions; // Initially, no filtering

        // Update view with all data
        updateView();

        // Update filter panel counts
        view.getFilterPanel().updateResultsLabel(
                filteredPredictions.size(),
                currentPredictions.size()
        );
    }
    private String generateExportFileName(String format) {
        FilterPanel filterPanel = view.getFilterPanel();
        String product = filterPanel.getProductFilter().getValue();
        String country = filterPanel.getCountryFilter().getValue();

        StringBuilder fileName = new StringBuilder("tunisian_exports");

        if (!"All Products".equals(product)) {
            fileName.append("_").append(product.replace(" ", "_").toLowerCase());
        }

        if (!"All Countries".equals(country)) {
            fileName.append("_").append(country.toLowerCase());
        }

        fileName.append("_").append(java.time.LocalDate.now().toString());
        fileName.append(".").append(format);

        return fileName.toString();
    }

    private void showExportNotification(String result, String format) {
        FilterPanel filterPanel = view.getFilterPanel();

        if (result.startsWith("✅")) {
            // Success
            filterPanel.updateFilterStatus(format + " export successful!");

            // Show success in console
            System.out.println(result);

            // You could also show an alert dialog here:
            // showAlert("Export Successful", result);

        } else if (result.startsWith("⚠️")) {
            // Warning
            filterPanel.updateFilterStatus("Export warning: " + result);
            System.out.println("⚠️ " + result);

        } else if (result.startsWith("❌")) {
            // Error
            filterPanel.updateFilterStatus("Export failed!");
            System.err.println("❌ " + result);
        }
    }

    private void exportFilteredData(String format) {
        if (filteredPredictions.isEmpty()) {
            System.out.println("⚠️ No data to export");
            view.getFilterPanel().updateFilterStatus("No data to export");
            return;
        }

        String fileName = generateExportFileName(format.toLowerCase());
        String result = exportService.exportFilteredData(filteredPredictions, format, fileName);
        showExportNotification(result, format);
    }
    private void applyFilters() {
        FilterPanel filterPanel = view.getFilterPanel();

        String selectedProduct = filterPanel.getProductFilter().getValue();
        String selectedCountry = filterPanel.getCountryFilter().getValue();
        LocalDate startDate = filterPanel.getStartDatePicker().getValue();
        LocalDate endDate = filterPanel.getEndDatePicker().getValue();

        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            System.out.println("⚠️ Invalid date range: Start date is after end date");
            filterPanel.updateFilterStatus("Invalid date range");
            return;
        }

        // Apply filters using FilterService
        filteredPredictions = filterService.filterPredictions(
                currentPredictions,
                selectedProduct,
                startDate,
                endDate,
                0.0 // Minimum confidence
        );

        // Build filter status message
        StringBuilder status = new StringBuilder();
        if (!"All Products".equals(selectedProduct)) {
            status.append("Product: ").append(selectedProduct).append("; ");
        }
        if (startDate != null && endDate != null) {
            status.append("Date: ").append(startDate).append(" to ").append(endDate).append("; ");
        }

        if (status.length() == 0) {
            filterPanel.updateFilterStatus("None (Showing all)");
        } else {
            filterPanel.updateFilterStatus(status.toString());
        }

        // Update results label
        filterPanel.updateResultsLabel(filteredPredictions.size(), currentPredictions.size());

        // Log filter summary
        System.out.println("✅ Filters applied successfully:");
        System.out.println("   Total records: " + currentPredictions.size());
        System.out.println("   Filtered records: " + filteredPredictions.size());
        System.out.println("   Filter criteria: " + status.toString());

        // Update dashboard with filtered data
        updateView();
        updateCharts();
    }

    private void resetFilters() {
        // Reset to show all data
        filteredPredictions = currentPredictions;

        // Update filter panel
        FilterPanel filterPanel = view.getFilterPanel();
        filterPanel.updateFilterStatus("None (Showing all)");
        filterPanel.updateResultsLabel(filteredPredictions.size(), currentPredictions.size());

        // Update dashboard
        updateView();

        System.out.println("✅ Filters reset - Showing all " + currentPredictions.size() + " records");
    }

    private void updateView() {
        // Calculate statistics with filtered data
        DashboardStatistics stats = statisticsService.calculateStatistics(filteredPredictions);

        // Enhance with real distributions from filtered data
        stats.setProductDistribution(dataService.getProductPriceDistribution());
        stats.setCountryDistribution(dataService.getCountryDistribution());

        // Update main dashboard view
        view.updateDashboard(stats, filteredPredictions);

        // UPDATE CHARTS WITH FILTERED DATA
        updateCharts();
    }

    private void refreshData() {
        System.out.println("🔄 Refreshing data...");
        loadInitialData();

        FilterPanel filterPanel = view.getFilterPanel();
        filterPanel.updateResultsLabel(filteredPredictions.size(), currentPredictions.size());
        filterPanel.updateFilterStatus("Data refreshed");

        // Refresh charts
        updateCharts();

        System.out.println("✅ Data refreshed - " + currentPredictions.size() + " records loaded");
    }
    private void updateCharts() {
        // Get historical data for time series charts
        List<ExportData> historicalData = dataService.loadHistoricalData();

        // Update all chart panels
        view.updateCharts(filteredPredictions, historicalData);

        System.out.println("📈 Charts updated with " + filteredPredictions.size() + " predictions");
    }

    private void saveFilterPreset() {
        FilterPanel filterPanel = view.getFilterPanel();

        String product = filterPanel.getProductFilter().getValue();
        String country = filterPanel.getCountryFilter().getValue();
        LocalDate startDate = filterPanel.getStartDatePicker().getValue();
        LocalDate endDate = filterPanel.getEndDatePicker().getValue();

        System.out.println("💾 Saving filter preset:");
        System.out.println("   Product: " + product);
        System.out.println("   Country: " + country);
        System.out.println("   Date Range: " + startDate + " to " + endDate);

        // TODO: Implement actual preset saving (to file or database)
        filterPanel.updateFilterStatus("Preset saved (placeholder)");
    }

    private void loadFilterPreset() {
        System.out.println("📂 Loading filter preset (placeholder)");

        // TODO: Implement actual preset loading

        // For now, just show a message
        view.getFilterPanel().updateFilterStatus("Preset loaded (placeholder)");
    }

    public void initialize() {
        loadInitialData();
        view.show();
    }
}