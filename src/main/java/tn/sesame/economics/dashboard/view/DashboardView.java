package tn.sesame.economics.dashboard.view;

import tn.sesame.economics.dashboard.view.chart.InteractiveChartPanel;
import tn.sesame.economics.dashboard.service.ChartDataService;
import tn.sesame.economics.dashboard.service.DataService;
import tn.sesame.economics.dashboard.view.FilterPanel;
import tn.sesame.economics.dashboard.view.StatisticsPanel;
import tn.sesame.economics.dashboard.view.ProductDistributionPanel;
import tn.sesame.economics.dashboard.view.CountryDistributionPanel;
import tn.sesame.economics.dashboard.view.TimeTrendsPanel;
import tn.sesame.economics.dashboard.model.DashboardStatistics;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ExportData; // ADDED
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;
import java.util.Map;
import tn.sesame.economics.dashboard.view.PredictiveAnalyticsDashboard;

/**
 * Main dashboard view
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

    public DashboardView(Stage primaryStage, DataService dataService) {
        this.stage = primaryStage;
        this.dataService = dataService;
        this.filterPanel = new FilterPanel();
        this.chartDataService = new ChartDataService();
        initializeUI();
    }

    private void initializeUI() {
        // Create main container
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

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

        mainTabs.getTabs().addAll(statsTab, chartsTab, analyticsTab);
        root.setCenter(mainTabs);

        // Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Create scene
        Scene scene = new Scene(root, 1400, 850);
        stage.setScene(scene);
        stage.setTitle("Tunisian Economic Intelligence Dashboard");
    }

    private VBox createStatisticsTab() {
        VBox statsTab = new VBox(20);
        statsTab.setPadding(new Insets(20));

        GridPane centerGrid = new GridPane();
        centerGrid.setHgap(20);
        centerGrid.setVgap(20);

        statisticsPanel = new StatisticsPanel();
        productPanel = new ProductDistributionPanel();
        countryPanel = new CountryDistributionPanel();
        trendsPanel = new TimeTrendsPanel(dataService);

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
        refreshChartsButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        refreshChartsButton.setOnAction(event -> refreshAllCharts());

        Button exportAllButton = new Button("📥 Export All Charts");
        exportAllButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        exportAllButton.setOnAction(event -> exportAllCharts());

        controls.getChildren().addAll(title, refreshChartsButton, exportAllButton);
        return controls;
    }

    private VBox createAnalyticsTab() {
        VBox analyticsTab = new VBox();
        analyticsTab.setPadding(new Insets(10));

        // Create the predictive analytics tab - placeholder for now
        Label placeholder = new Label("🤖 Predictive Analytics Dashboard\n\n" +
                "This feature will include:\n" +
                "• Real-time price prediction interface\n" +
                "• Batch prediction processing\n" +
                "• What-if scenario analysis\n" +
                "• Prediction history tracking\n\n" +
                "Implementation coming soon!");
        placeholder.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");
        placeholder.setWrapText(true);

        analyticsTab.getChildren().add(placeholder);
        PredictiveAnalyticsDashboard analyticsDashboard = new PredictiveAnalyticsDashboard(dataService);
        analyticsTab.getChildren().add(analyticsDashboard);

        return analyticsTab;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #2c3e50;");

        Label title = new Label("🤖 TUNISIAN AGRICULTURAL EXPORT INTELLIGENCE SYSTEM");
        title.setFont(Font.font("Arial", 24));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Real-time Market Analysis & Price Predictions");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.LIGHTGRAY);

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox createPlaceholderPanel(String title) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #fff8dc; -fx-border-color: #dda; -fx-border-radius: 5;");

        Label label = new Label(title);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.DARKSLATEGRAY);

        Label placeholder = new Label("(Feature coming soon)");
        placeholder.setTextFill(Color.GRAY);

        panel.getChildren().addAll(label, placeholder);
        return panel;
    }

    private VBox createInfoPanel() {
        VBox infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(15));
        infoPanel.setStyle("-fx-background-color: #fff8e1; -fx-border-color: #ffd54f; -fx-border-radius: 5;");

        Label title = new Label("ℹ️ FILTERING GUIDE");
        title.setFont(Font.font("Arial", 14));
        title.setTextFill(Color.DARKSLATEGRAY);

        TextArea guide = new TextArea();
        guide.setText("How to use filters:\n" +
                "1. Select product type\n" +
                "2. Choose destination country\n" +
                "3. Set date range\n" +
                "4. Click 'Apply Filters'\n\n" +
                "You can save filter combinations\n" +
                "as presets for later use.");
        guide.setEditable(false);
        guide.setWrapText(true);
        guide.setPrefHeight(120);
        guide.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        infoPanel.getChildren().addAll(title, guide);
        return infoPanel;
    }

    private HBox createFooter() {
        HBox footer = new HBox(20);
        footer.setPadding(new Insets(10));
        footer.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7;");

        statusLabel = new Label("Dashboard loaded successfully");
        statusLabel.setTextFill(Color.DARKSLATEGRAY);

        refreshButton = new Button("🔄 Refresh Data");
        refreshButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8px 16px;");

        backButton = new Button("⬅ Back to Main Menu");
        backButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 8px 16px;");

        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        footer.getChildren().addAll(statusLabel, refreshButton, backButton);

        return footer;
    }

    public void updateDashboard(DashboardStatistics stats, List<PricePrediction> predictions) {
        statisticsPanel.updateStatistics(stats);
        productPanel.updateProductDistribution(stats.getProductDistribution());
        countryPanel.updateCountryDistribution(stats.getCountryDistribution());
        trendsPanel.updateTrends();
        statusLabel.setText("Last updated: " + java.time.LocalTime.now().toString() +
                " | " + predictions.size() + " predictions loaded");
    }

    public void updateCharts(List<PricePrediction> predictions, List<ExportData> historicalData) {
        if (priceChartPanel != null) {
            // Update price distribution chart
            Map<String, Double> priceData = chartDataService.prepareProductPriceChart(predictions);
            priceChartPanel.setChartData(priceData, "Product Price Distribution (TND/tonne)");
        }

        if (trendChartPanel != null && historicalData != null) {
            // Update time series chart
            Map<String, Double> timeSeriesData = chartDataService.prepareTimeSeriesChart(historicalData);
            trendChartPanel.setChartData(timeSeriesData, "Monthly Price Trends");
        }

        if (comparisonChartPanel != null) {
            // Update product comparison chart
            Map<String, Double> comparisonData = chartDataService.prepareProductComparisonChart(predictions);
            comparisonChartPanel.setChartData(comparisonData, "Product Price Comparison");
        }
    }

    private void refreshAllCharts() {
        System.out.println("Refreshing all charts...");
        // This will be implemented by the controller
    }

    private void exportAllCharts() {
        // Export all three charts
        if (priceChartPanel != null) {
            // Trigger the export button on the panel
            priceChartPanel.getExportImageButton().fire();
        }
        if (trendChartPanel != null) {
            trendChartPanel.getExportImageButton().fire();
        }
        if (comparisonChartPanel != null) {
            comparisonChartPanel.getExportImageButton().fire();
        }

        // Show notification
        statusLabel.setText("✅ Exporting all charts...");
        System.out.println("Exporting all charts...");
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