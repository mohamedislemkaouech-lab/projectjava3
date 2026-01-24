package tn.sesame.economics.dashboard.view;

import javafx.scene.control.*;
import tn.sesame.economics.dashboard.view.chart.InteractiveChartPanel;
import tn.sesame.economics.dashboard.service.ChartDataService;
import tn.sesame.economics.dashboard.service.DataService;
import tn.sesame.economics.dashboard.service.ReportService;
import tn.sesame.economics.dashboard.view.FilterPanel;
import tn.sesame.economics.dashboard.view.StatisticsPanel;
import tn.sesame.economics.dashboard.view.ProductDistributionPanel;
import tn.sesame.economics.dashboard.view.CountryDistributionPanel;
import tn.sesame.economics.dashboard.view.TimeTrendsPanel;
import tn.sesame.economics.dashboard.model.DashboardStatistics;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ExportData;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;
import java.util.Map;
import tn.sesame.economics.dashboard.view.PredictiveAnalyticsDashboard;

/**
 * Main dashboard view with integrated Report Generation
 */
public class DashboardView {

    private Stage stage;
    private DataService dataService;
    private StatisticsPanel statisticsPanel;
    private ProductDistributionPanel productPanel;
    private CountryDistributionPanel countryPanel;
    private TimeTrendsPanel trendsPanel;
    private Label statusLabel;
    private Button refreshButton;
    private Button backButton;
    private FilterPanel filterPanel;
    private TabPane chartTabs;
    private InteractiveChartPanel priceChartPanel;
    private InteractiveChartPanel trendChartPanel;
    private InteractiveChartPanel comparisonChartPanel;
    private ChartDataService chartDataService;

    // Report Generation Components
    private ReportService reportService;
    private ReportGenerationDashboard reportDashboard;

    public DashboardView(Stage primaryStage, DataService dataService) {
        this.stage = primaryStage;
        this.dataService = dataService;
        this.filterPanel = new FilterPanel();
        this.chartDataService = new ChartDataService();

        // Initialize Report Service
        this.reportService = new ReportService(true); // Use local LLM

        initializeUI();
    }

    private void initializeUI() {
        // Create main container
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.getStyleClass().add("dashboard-root");

        // Header
        VBox header = createHeader();
        root.setTop(header);

        // LEFT: Filter Panel
        VBox leftPanel = new VBox();
        leftPanel.setPadding(new Insets(10));
        leftPanel.setSpacing(10);
        leftPanel.setPrefWidth(300);

        leftPanel.getChildren().add(filterPanel);
        leftPanel.getChildren().add(createInfoPanel());

        root.setLeft(leftPanel);

        // CENTER: Main content area with tabs
        TabPane mainTabs = new TabPane();

        // Tab 1: Statistics Dashboard
        Tab statsTab = new Tab("📊 Statistics Dashboard");
        statsTab.setClosable(false);
        statsTab.setContent(createStatisticsTab());

        // Tab 2: Chart Visualizations
        Tab chartsTab = new Tab("📈 Advanced Charts");
        chartsTab.setClosable(false);
        chartsTab.setContent(createChartsTab());

        // Tab 3: Predictive Analytics
        Tab analyticsTab = new Tab("🤖 Predictive Analytics");
        analyticsTab.setClosable(false);
        analyticsTab.setContent(createAnalyticsTab());

        // Tab 4: Report Generation (NEW - INTEGRATED)
        Tab reportTab = new Tab("📄 Report Generation");
        reportTab.setClosable(false);
        reportTab.setContent(createReportTab());

        mainTabs.getTabs().addAll(statsTab, chartsTab, analyticsTab, reportTab);
        root.setCenter(mainTabs);

        // Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Create scene with CSS
        Scene scene = new Scene(root, 1400, 850);

        // Load CSS stylesheet
        try {
            // Try to load from resources
            String css = getClass().getResource("/dashboard.css").toExternalForm();
            scene.getStylesheets().add(css);
            System.out.println("✅ CSS stylesheet loaded successfully");
        } catch (Exception e) {
            System.out.println("⚠️ CSS stylesheet not found, using default styling");
            // Apply inline styles as fallback
            applyInlineStyles(root);
        }

        stage.setScene(scene);
        stage.setTitle("🤖 Tunisian Economic Intelligence Dashboard");
    }

    private VBox createStatisticsTab() {
        VBox statsTab = new VBox(20);
        statsTab.setPadding(new Insets(20));
        statsTab.getStyleClass().add("tab-content");

        GridPane centerGrid = new GridPane();
        centerGrid.setHgap(20);
        centerGrid.setVgap(20);

        statisticsPanel = new StatisticsPanel();
        statisticsPanel.getStyleClass().add("statistics-panel");

        productPanel = new ProductDistributionPanel();
        productPanel.getStyleClass().add("product-distribution-panel");

        countryPanel = new CountryDistributionPanel();
        countryPanel.getStyleClass().add("country-distribution-panel");

        trendsPanel = new TimeTrendsPanel(dataService);
        trendsPanel.getStyleClass().add("trends-panel");

        // Row 0: Statistics and Product Distribution
        centerGrid.add(statisticsPanel, 0, 0);
        centerGrid.add(productPanel, 1, 0);

        // Row 1: Country Distribution and Time Trends
        centerGrid.add(countryPanel, 0, 1);
        centerGrid.add(trendsPanel, 1, 1);

        statsTab.getChildren().add(centerGrid);
        return statsTab;
    }

    private VBox createChartsTab() {
        VBox chartsTab = new VBox(20);
        chartsTab.setPadding(new Insets(20));
        chartsTab.getStyleClass().add("tab-content");

        // Create chart selection controls
        HBox chartControls = createChartControls();

        // Create chart tabs
        chartTabs = new TabPane();

        // Tab 1: Price Charts
        Tab priceTab = new Tab("💰 Price Charts");
        priceTab.setClosable(false);
        priceChartPanel = new InteractiveChartPanel();
        priceTab.setContent(priceChartPanel);

        // Tab 2: Trend Charts
        Tab trendTab = new Tab("📈 Trend Analysis");
        trendTab.setClosable(false);
        trendChartPanel = new InteractiveChartPanel();
        trendTab.setContent(trendChartPanel);

        // Tab 3: Comparison Charts
        Tab compareTab = new Tab("⚖️ Comparisons");
        compareTab.setClosable(false);
        comparisonChartPanel = new InteractiveChartPanel();
        compareTab.setContent(comparisonChartPanel);

        chartTabs.getTabs().addAll(priceTab, trendTab, compareTab);

        chartsTab.getChildren().addAll(chartControls, chartTabs);
        return chartsTab;
    }

    private HBox createChartControls() {
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: #f0f8ff; -fx-border-radius: 5;");

        Label title = new Label("🎨 CHART CONTROLS");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button refreshChartsButton = new Button("🔄 Refresh All Charts");
        refreshChartsButton.getStyleClass().addAll("button", "button-success");
        refreshChartsButton.setOnAction(event -> refreshAllCharts());

        Button exportAllButton = new Button("📥 Export All Charts");
        exportAllButton.getStyleClass().addAll("button", "button-info");
        exportAllButton.setOnAction(event -> exportAllCharts());

        controls.getChildren().addAll(title, refreshChartsButton, exportAllButton);
        return controls;
    }

    private VBox createAnalyticsTab() {
        VBox analyticsTab = new VBox();
        analyticsTab.setPadding(new Insets(10));
        analyticsTab.getStyleClass().add("tab-content");

        PredictiveAnalyticsDashboard analyticsDashboard = new PredictiveAnalyticsDashboard(dataService);
        analyticsTab.getChildren().add(analyticsDashboard);

        return analyticsTab;
    }

    /**
     * NEW: Create Report Generation Tab (Integrated)
     */
    private VBox createReportTab() {
        // Create the report dashboard
        reportDashboard = new ReportGenerationDashboard(reportService);

        // Load data
        List<PricePrediction> predictions = dataService.getPredictions();
        List<ExportData> historicalData = dataService.loadHistoricalData();
        reportDashboard.setData(predictions, historicalData);

        // Create ScrollPane with proper styling
        ScrollPane scrollPane = new ScrollPane(reportDashboard);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("report-tab-scroll");

        // Force scrollbars
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Create container
        VBox container = new VBox(scrollPane);
        container.getStyleClass().add("report-generation-tab");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return container;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.getStyleClass().add("dashboard-header");

        Label title = new Label("🤖 TUNISIAN AGRICULTURAL EXPORT INTELLIGENCE SYSTEM");
        title.getStyleClass().add("dashboard-title");

        Label subtitle = new Label("Real-time Market Analysis, Price Predictions & Intelligent Reporting");
        subtitle.getStyleClass().add("dashboard-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox createInfoPanel() {
        VBox infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(15));
        infoPanel.setStyle("-fx-background-color: #fff8e1; -fx-border-color: #ffd54f; -fx-border-radius: 5;");

        Label title = new Label("ℹ️ QUICK GUIDE");
        title.setFont(Font.font("Arial", 14));
        title.setTextFill(Color.DARKSLATEGRAY);

        TextArea guide = new TextArea();
        guide.setText("Dashboard Features:\n\n" +
                "📊 Statistics: View key metrics\n" +
                "📈 Charts: Interactive visualizations\n" +
                "🤖 Analytics: AI predictions\n" +
                "📄 Reports: Generate AI-powered reports\n\n" +
                "Use filters to refine data\n" +
                "Export charts and reports\n" +
                "Save presets for later use");
        guide.setEditable(false);
        guide.setWrapText(true);
        guide.setPrefHeight(150);
        guide.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        infoPanel.getChildren().addAll(title, guide);
        return infoPanel;
    }

    private HBox createFooter() {
        HBox footer = new HBox(20);
        footer.setPadding(new Insets(10));
        footer.getStyleClass().add("dashboard-footer");

        statusLabel = new Label("Dashboard loaded successfully - All systems operational");
        statusLabel.getStyleClass().add("status-label");

        refreshButton = new Button("🔄 Refresh Data");
        refreshButton.getStyleClass().addAll("button", "button-success");

        backButton = new Button("⬅ Back to Main Menu");
        backButton.getStyleClass().addAll("button", "button-warning");

        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        footer.getChildren().addAll(statusLabel, refreshButton, backButton);

        return footer;
    }

    public void updateDashboard(DashboardStatistics stats, List<PricePrediction> predictions) {
        statisticsPanel.updateStatistics(stats);
        productPanel.updateProductDistribution(stats.getProductDistribution());
        countryPanel.updateCountryDistribution(stats.getCountryDistribution());
        trendsPanel.updateTrends();

        // Update report dashboard with latest data
        if (reportDashboard != null) {
            List<ExportData> historicalData = dataService.loadHistoricalData();
            reportDashboard.setData(predictions, historicalData);
        }

        statusLabel.setText("Last updated: " + java.time.LocalTime.now().toString() +
                " | " + predictions.size() + " predictions loaded");
    }

    public void updateCharts(List<PricePrediction> predictions, List<ExportData> historicalData) {
        if (priceChartPanel != null) {
            Map<String, Double> priceData = chartDataService.prepareProductPriceChart(predictions);
            priceChartPanel.setChartData(priceData, "Product Price Distribution (TND/tonne)");
        }

        if (trendChartPanel != null && historicalData != null) {
            Map<String, Double> timeSeriesData = chartDataService.prepareTimeSeriesChart(historicalData);
            trendChartPanel.setChartData(timeSeriesData, "Monthly Price Trends");
        }

        if (comparisonChartPanel != null) {
            Map<String, Double> comparisonData = chartDataService.prepareProductComparisonChart(predictions);
            comparisonChartPanel.setChartData(comparisonData, "Product Price Comparison");
        }
    }

    private void refreshAllCharts() {
        System.out.println("Refreshing all charts...");
        statusLabel.setText("Refreshing charts...");
    }

    private void exportAllCharts() {
        if (priceChartPanel != null) {
            priceChartPanel.getExportImageButton().fire();
        }
        if (trendChartPanel != null) {
            trendChartPanel.getExportImageButton().fire();
        }
        if (comparisonChartPanel != null) {
            comparisonChartPanel.getExportImageButton().fire();
        }

        statusLabel.setText("✅ Exporting all charts...");
        System.out.println("Exporting all charts...");
    }

    /**
     * Apply inline styles as fallback if CSS file is not found
     */
    private void applyInlineStyles(BorderPane root) {
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public void show() {
        stage.show();
    }

    public void close() {
        // Clean up report service
        if (reportDashboard != null) {
            reportDashboard.shutdown();
        }
        stage.close();
    }

    public FilterPanel getFilterPanel() {
        return filterPanel;
    }

    public InteractiveChartPanel getPriceChartPanel() {
        return priceChartPanel;
    }

    public InteractiveChartPanel getTrendChartPanel() {
        return trendChartPanel;
    }

    public InteractiveChartPanel getComparisonChartPanel() {
        return comparisonChartPanel;
    }

    public ChartDataService getChartDataService() {
        return chartDataService;
    }
}