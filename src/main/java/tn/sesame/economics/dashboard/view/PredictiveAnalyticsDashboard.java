package tn.sesame.economics.dashboard.view;

import tn.sesame.economics.dashboard.service.DataService;
import tn.sesame.economics.model.*;
import tn.sesame.economics.service.EconomicIntelligenceService;
import tn.sesame.economics.ai.DJLPredictionService;
import tn.sesame.economics.ai.LLMReportService;
import tn.sesame.economics.model.PredictionStatus;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.BarChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Predictive Analytics Dashboard with all required functionalities
 */
public class PredictiveAnalyticsDashboard extends VBox {

    private final DataService dataService;
    private EconomicIntelligenceService intelligenceService;
    private ExecutorService executorService;

    // Real-time Prediction Section
    private ComboBox<ProductType> productCombo;
    private ComboBox<String> countryCombo;
    private Spinner<Double> priceSpinner;
    private Spinner<Double> volumeSpinner;
    private ComboBox<MarketIndicator> marketIndicatorCombo;
    private TextArea realTimeResults;
    private Button predictButton;
    private ProgressIndicator realTimeProgress;

    // Batch Prediction Section
    private ListView<PricePrediction> batchQueueList;
    private ProgressBar batchProgressBar;
    private Label batchProgressLabel;
    private Button startBatchButton;
    private Button pauseBatchButton;
    private Button cancelBatchButton;
    private TextField batchSizeField;
    private TableView<PricePrediction> batchResultsTable;

    // History & Comparison Section
    private ListView<PricePrediction> predictionHistoryList;
    private TableView<ComparisonItem> comparisonTable;
    private LineChart<String, Number> trendChart;
    private Button compareButton;
    private Button clearHistoryButton;
    private ObservableList<PricePrediction> predictionHistory;

    // What-if Scenario Section
    private ComboBox<ProductType> scenarioProductCombo;
    private Slider priceChangeSlider;
    private Slider volumeChangeSlider;
    private ComboBox<MarketIndicator> scenarioMarketCombo;
    private TextArea scenarioResults;
    private Button runScenarioButton;
    private Button saveScenarioButton;
    private Button loadScenarioButton;
    private BarChart<String, Number> scenarioChart;

    // Scenario storage
    private final Map<String, Map<String, Object>> savedScenarios;

    public PredictiveAnalyticsDashboard(DataService dataService) {
        this.dataService = dataService;
        this.executorService = Executors.newFixedThreadPool(2);
        this.predictionHistory = FXCollections.observableArrayList();
        this.savedScenarios = new HashMap<>();

        initializeServices();
        initializeUI();
        loadSampleHistory();
    }

    private void initializeServices() {
        // Initialize AI services
        try {
            var predictionService = new DJLPredictionService();
            predictionService.loadModel();
            var reportService = new LLMReportService(true); // Use local LLM
            this.intelligenceService = new EconomicIntelligenceService(predictionService, reportService);
        } catch (Exception e) {
            System.err.println("Failed to initialize AI services: " + e.getMessage());
            // Continue with mock data for demo
        }
    }

    private void initializeUI() {
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        // Create tab pane for organized sections
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Real-time Prediction
        Tab realTimeTab = createRealTimeTab();

        // Tab 2: Batch Processing
        Tab batchTab = createBatchTab();

        // Tab 3: History & Comparison
        Tab historyTab = createHistoryTab();

        // Tab 4: What-if Scenarios
        Tab scenarioTab = createScenarioTab();

        tabPane.getTabs().addAll(realTimeTab, batchTab, historyTab, scenarioTab);
        getChildren().add(tabPane);
    }

    private Tab createRealTimeTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #e8f5e9;");

        // Title
        Label title = new Label("🔮 REAL-TIME PRICE PREDICTION");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        // Input Form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));

        // Product Type
        formGrid.add(new Label("Product Type:"), 0, 0);
        productCombo = new ComboBox<>();
        productCombo.getItems().addAll(ProductType.values());
        productCombo.setValue(ProductType.OLIVE_OIL);
        productCombo.setPrefWidth(200);
        formGrid.add(productCombo, 1, 0);

        // Destination Country
        formGrid.add(new Label("Destination Country:"), 0, 1);
        countryCombo = new ComboBox<>();
        countryCombo.getItems().addAll("France", "Germany", "Italy", "Spain", "UK", "USA", "Tunisia");
        countryCombo.setValue("France");
        formGrid.add(countryCombo, 1, 1);

        // Current Price
        formGrid.add(new Label("Current Price (TND/ton):"), 0, 2);
        priceSpinner = new Spinner<>(0.0, 10000.0, 3500.0, 100.0);
        priceSpinner.setEditable(true);
        priceSpinner.setPrefWidth(150);
        formGrid.add(priceSpinner, 1, 2);

        // Volume
        formGrid.add(new Label("Volume (tons):"), 0, 3);
        volumeSpinner = new Spinner<>(1.0, 1000.0, 100.0, 10.0);
        volumeSpinner.setEditable(true);
        formGrid.add(volumeSpinner, 1, 3);

        // Market Indicator
        formGrid.add(new Label("Market Condition:"), 0, 4);
        marketIndicatorCombo = new ComboBox<>();
        marketIndicatorCombo.getItems().addAll(MarketIndicator.values());
        marketIndicatorCombo.setValue(MarketIndicator.STABLE);
        formGrid.add(marketIndicatorCombo, 1, 4);

        // Prediction Button
        predictButton = new Button("🤖 PREDICT FUTURE PRICE");
        predictButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 20px;");
        predictButton.setOnAction(e -> performRealTimePrediction());

        // Progress Indicator
        realTimeProgress = new ProgressIndicator();
        realTimeProgress.setVisible(false);
        realTimeProgress.setPrefSize(30, 30);

        HBox buttonBox = new HBox(10, predictButton, realTimeProgress);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // Results Area
        realTimeResults = new TextArea();
        realTimeResults.setPrefHeight(200);
        realTimeResults.setEditable(false);
        realTimeResults.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
        realTimeResults.setText("Enter parameters and click 'Predict' to see results here...");

        // Add components
        tabContent.getChildren().addAll(title, formGrid, buttonBox, realTimeResults);

        Tab tab = new Tab("🔮 Real-time", tabContent);
        tab.setTooltip(new Tooltip("Make individual price predictions"));
        return tab;
    }

    private Tab createBatchTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #e3f2fd;");

        // Title
        Label title = new Label("📦 BATCH PREDICTION PROCESSING");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1565c0;");

        // Batch Controls
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label batchSizeLabel = new Label("Batch Size:");
        batchSizeField = new TextField("10");
        batchSizeField.setPrefWidth(80);

        startBatchButton = createControlButton("▶ Start Batch", "#4caf50");
        pauseBatchButton = createControlButton("⏸ Pause", "#ff9800");
        cancelBatchButton = createControlButton("⏹ Cancel", "#f44336");

        pauseBatchButton.setDisable(true);
        cancelBatchButton.setDisable(true);

        controls.getChildren().addAll(batchSizeLabel, batchSizeField,
                startBatchButton, pauseBatchButton, cancelBatchButton);

        // Progress Bar
        batchProgressBar = new ProgressBar(0);
        batchProgressBar.setPrefWidth(400);
        batchProgressLabel = new Label("0% - Ready");

        VBox progressBox = new VBox(5, batchProgressBar, batchProgressLabel);

        // Queue List
        Label queueLabel = new Label("Prediction Queue:");
        batchQueueList = new ListView<>();
        batchQueueList.setPrefHeight(150);

        // Results Table
        Label resultsLabel = new Label("Batch Results:");
        batchResultsTable = new TableView<>();
        batchResultsTable.setPrefHeight(200);

        // Setup table columns
        TableColumn<PricePrediction, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().productType().getFrenchName()));

        TableColumn<PricePrediction, Double> priceCol = new TableColumn<>("Predicted Price");
        priceCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(
                        cell.getValue().predictedPrice()).asObject());

        TableColumn<PricePrediction, Double> confidenceCol = new TableColumn<>("Confidence");
        confidenceCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(
                        cell.getValue().confidence()).asObject());

        batchResultsTable.getColumns().addAll(productCol, priceCol, confidenceCol);

        // Event Handlers
        startBatchButton.setOnAction(e -> startBatchProcessing());
        pauseBatchButton.setOnAction(e -> toggleBatchProcessing());
        cancelBatchButton.setOnAction(e -> cancelBatchProcessing());

        // Add components
        tabContent.getChildren().addAll(title, controls, progressBox,
                queueLabel, batchQueueList,
                resultsLabel, batchResultsTable);

        Tab tab = new Tab("📦 Batch Processing", tabContent);
        tab.setTooltip(new Tooltip("Process multiple predictions at once"));
        return tab;
    }

    private Tab createHistoryTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #fff3e0;");

        // Title
        Label title = new Label("📊 PREDICTION HISTORY & COMPARISON");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ef6c00;");

        // Two-column layout
        HBox mainContent = new HBox(20);

        // Left: History List
        VBox historyBox = new VBox(10);
        historyBox.setPrefWidth(300);

        Label historyLabel = new Label("Prediction History:");
        predictionHistoryList = new ListView<>(predictionHistory);
        predictionHistoryList.setPrefHeight(300);
        predictionHistoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        HBox historyButtons = new HBox(10);
        compareButton = createControlButton("📊 Compare Selected", "#2196f3");
        clearHistoryButton = createControlButton("🗑 Clear History", "#f44336");

        compareButton.setOnAction(e -> compareSelectedPredictions());
        clearHistoryButton.setOnAction(e -> clearHistory());

        historyButtons.getChildren().addAll(compareButton, clearHistoryButton);

        historyBox.getChildren().addAll(historyLabel, predictionHistoryList, historyButtons);

        // Right: Comparison and Chart
        VBox comparisonBox = new VBox(10);
        comparisonBox.setPrefWidth(400);

        Label comparisonLabel = new Label("Comparison Table:");
        comparisonTable = new TableView<>();
        comparisonTable.setPrefHeight(150);

        // Comparison table columns
        TableColumn<ComparisonItem, String> itemCol = new TableColumn<>("Metric");
        itemCol.setCellValueFactory(cell -> cell.getValue().metricProperty());

        TableColumn<ComparisonItem, String> value1Col = new TableColumn<>("Prediction 1");
        value1Col.setCellValueFactory(cell -> cell.getValue().value1Property());

        TableColumn<ComparisonItem, String> value2Col = new TableColumn<>("Prediction 2");
        value2Col.setCellValueFactory(cell -> cell.getValue().value2Property());

        comparisonTable.getColumns().addAll(itemCol, value1Col, value2Col);

        // Trend Chart
        Label trendLabel = new Label("Price Trend Analysis:");
        trendChart = createTrendChart();
        trendChart.setPrefHeight(200);

        comparisonBox.getChildren().addAll(comparisonLabel, comparisonTable, trendLabel, trendChart);

        mainContent.getChildren().addAll(historyBox, comparisonBox);

        tabContent.getChildren().addAll(title, mainContent);

        Tab tab = new Tab("📊 History & Comparison", tabContent);
        tab.setTooltip(new Tooltip("View and compare historical predictions"));
        return tab;
    }

    private Tab createScenarioTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #f3e5f5;");

        // Title
        Label title = new Label("🧪 WHAT-IF SCENARIO ANALYSIS");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7b1fa2;");

        // Two-column layout
        HBox mainContent = new HBox(20);

        // Left: Scenario Controls
        VBox controlsBox = new VBox(15);
        controlsBox.setPrefWidth(300);

        // Scenario parameters
        GridPane scenarioGrid = new GridPane();
        scenarioGrid.setHgap(10);
        scenarioGrid.setVgap(10);

        scenarioGrid.add(new Label("Product:"), 0, 0);
        scenarioProductCombo = new ComboBox<>();
        scenarioProductCombo.getItems().addAll(ProductType.values());
        scenarioProductCombo.setValue(ProductType.OLIVE_OIL);
        scenarioGrid.add(scenarioProductCombo, 1, 0);

        scenarioGrid.add(new Label("Price Change (%):"), 0, 1);
        priceChangeSlider = new Slider(-50, 50, 0);
        priceChangeSlider.setShowTickLabels(true);
        priceChangeSlider.setShowTickMarks(true);
        priceChangeSlider.setMajorTickUnit(25);
        priceChangeSlider.setBlockIncrement(5);
        scenarioGrid.add(priceChangeSlider, 1, 1);

        Label priceChangeLabel = new Label("0%");
        priceChangeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                priceChangeLabel.setText(String.format("%.0f%%", newVal)));
        scenarioGrid.add(priceChangeLabel, 2, 1);

        scenarioGrid.add(new Label("Volume Change (%):"), 0, 2);
        volumeChangeSlider = new Slider(-50, 50, 0);
        volumeChangeSlider.setShowTickLabels(true);
        volumeChangeSlider.setShowTickMarks(true);
        scenarioGrid.add(volumeChangeSlider, 1, 2);

        Label volumeChangeLabel = new Label("0%");
        volumeChangeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                volumeChangeLabel.setText(String.format("%.0f%%", newVal)));
        scenarioGrid.add(volumeChangeLabel, 2, 2);

        scenarioGrid.add(new Label("Market Condition:"), 0, 3);
        scenarioMarketCombo = new ComboBox<>();
        scenarioMarketCombo.getItems().addAll(MarketIndicator.values());
        scenarioMarketCombo.setValue(MarketIndicator.STABLE);
        scenarioGrid.add(scenarioMarketCombo, 1, 3);

        controlsBox.getChildren().add(scenarioGrid);

        // Scenario buttons
        HBox scenarioButtons = new HBox(10);
        runScenarioButton = createControlButton("🧪 Run Scenario", "#9c27b0");
        saveScenarioButton = createControlButton("💾 Save Scenario", "#607d8b");
        loadScenarioButton = createControlButton("📂 Load Scenario", "#795548");

        runScenarioButton.setOnAction(e -> runScenarioAnalysis());
        saveScenarioButton.setOnAction(e -> saveCurrentScenario());
        loadScenarioButton.setOnAction(e -> loadSavedScenario());

        scenarioButtons.getChildren().addAll(runScenarioButton, saveScenarioButton, loadScenarioButton);
        controlsBox.getChildren().add(scenarioButtons);

        // Right: Results and Chart
        VBox resultsBox = new VBox(15);

        scenarioResults = new TextArea();
        scenarioResults.setPrefHeight(200);
        scenarioResults.setEditable(false);
        scenarioResults.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 12px;");

        // Scenario Chart
        scenarioChart = createScenarioChart();
        scenarioChart.setPrefHeight(200);

        resultsBox.getChildren().addAll(new Label("Scenario Results:"), scenarioResults,
                new Label("Impact Analysis:"), scenarioChart);

        mainContent.getChildren().addAll(controlsBox, resultsBox);
        tabContent.getChildren().addAll(title, mainContent);

        Tab tab = new Tab("🧪 What-if Scenarios", tabContent);
        tab.setTooltip(new Tooltip("Simulate different market conditions"));
        return tab;
    }

    private Button createControlButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 8px 12px;",
                color
        ));
        return button;
    }

    private LineChart<String, Number> createTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price (TND)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Price Trend Analysis");
        chart.setLegendVisible(false);

        // Sample data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Jan", 3200));
        series.getData().add(new XYChart.Data<>("Feb", 3400));
        series.getData().add(new XYChart.Data<>("Mar", 3600));
        series.getData().add(new XYChart.Data<>("Apr", 3800));
        series.getData().add(new XYChart.Data<>("May", 4000));

        chart.getData().add(series);
        return chart;
    }

    private BarChart<String, Number> createScenarioChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Impact (%)");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Scenario Impact Analysis");

        return chart;
    }

    private void performRealTimePrediction() {
        try {
            // Show progress
            realTimeProgress.setVisible(true);
            predictButton.setDisable(true);

            // Get input values
            ExportData input = new ExportData(
                    LocalDate.now().plusDays(30), // Predict for 30 days ahead
                    productCombo.getValue(),
                    priceSpinner.getValue(),
                    volumeSpinner.getValue(),
                    countryCombo.getValue(),
                    marketIndicatorCombo.getValue()
            );

            // Execute prediction in background thread
            executorService.submit(() -> {
                try {
                    // Use AI service for prediction
                    List<ExportData> singleInput = List.of(input);
                    List<PricePrediction> predictions = intelligenceService.analyzeExports(singleInput);

                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        if (!predictions.isEmpty()) {
                            PricePrediction prediction = predictions.get(0);

                            // Format results
                            StringBuilder result = new StringBuilder();
                            result.append("✅ PREDICTION SUCCESSFUL\n");
                            result.append("══════════════════════════\n\n");
                            result.append(String.format("📅 Prediction Date: %s\n",
                                    prediction.predictionDate()));
                            result.append(String.format("🏷️  Product: %s\n",
                                    prediction.productType().getFrenchName()));
                            result.append(String.format("💰 Predicted Price: %.2f TND/ton\n",
                                    prediction.predictedPrice()));
                            result.append(String.format("📊 Confidence: %.1f%%\n",
                                    prediction.confidence() * 100));
                            result.append(String.format("🤖 Model: %s\n",
                                    prediction.modelName()));
                            result.append(String.format("📈 Status: %s\n",
                                    prediction.status()));
                            result.append(String.format("⚠️  Risk Level: %d (%s)\n",
                                    prediction.getRiskLevel(), prediction.getRiskDescription()));

                            result.append("\n💡 RECOMMENDATIONS:\n");
                            if (prediction.confidence() >= 0.8) {
                                result.append("• High confidence prediction - Consider immediate action\n");
                                result.append("• Lock in prices for maximum profit\n");
                            } else if (prediction.confidence() >= 0.6) {
                                result.append("• Moderate confidence - Monitor market closely\n");
                                result.append("• Consider partial hedging\n");
                            } else {
                                result.append("• Low confidence - Gather more data\n");
                                result.append("• Consider diversifying products\n");
                            }

                            realTimeResults.setText(result.toString());

                            // Add to history
                            predictionHistory.add(prediction);
                            updateTrendChart(prediction);

                        } else {
                            realTimeResults.setText("❌ No prediction returned from model");
                        }

                        // Hide progress
                        realTimeProgress.setVisible(false);
                        predictButton.setDisable(false);
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        realTimeResults.setText("❌ Prediction Error: " + e.getMessage());
                        realTimeProgress.setVisible(false);
                        predictButton.setDisable(false);
                    });
                }
            });

        } catch (Exception e) {
            realTimeResults.setText("❌ Error: " + e.getMessage());
            realTimeProgress.setVisible(false);
            predictButton.setDisable(false);
        }
    }

    private void startBatchProcessing() {
        try {
            int batchSize = Integer.parseInt(batchSizeField.getText());
            if (batchSize <= 0) {
                batchProgressLabel.setText("Invalid batch size");
                return;
            }

            // Update UI state
            startBatchButton.setDisable(true);
            pauseBatchButton.setDisable(false);
            cancelBatchButton.setDisable(false);

            // Change cancel button text to indicate it's active
            cancelBatchButton.setText("⏹ Cancel (Active)");

            // Clear previous results
            batchQueueList.getItems().clear();
            batchResultsTable.getItems().clear();

            // Generate sample batch data
            List<ExportData> batchData = generateBatchData(batchSize);

            // Add to queue
            for (int i = 0; i < batchData.size(); i++) {
                batchQueueList.getItems().add(
                        PricePrediction.pendingPrediction(
                                LocalDate.now().plusDays(i * 7),
                                batchData.get(i).productType(),
                                "DJL-Batch-Model"
                        )
                );
            }

            // Start processing in background
            executorService.submit(() -> processBatch(batchData));

        } catch (NumberFormatException e) {
            batchProgressLabel.setText("Invalid batch size format");
        }
    }

    private void processBatch(List<ExportData> batchData) {
        AtomicInteger completed = new AtomicInteger(0);
        int total = batchData.size();

        for (int i = 0; i < total; i++) {
            // Create a final copy of the loop variable for use in lambda
            final int currentIndex = i;

            // Check if cancelled
            if (!cancelBatchButton.isDisabled()) { // If cancel button is NOT disabled (enabled), processing is active
                try {
                    // Simulate processing time
                    Thread.sleep(500);

                    // Get prediction
                    List<PricePrediction> predictions = intelligenceService.analyzeExports(
                            List.of(batchData.get(currentIndex))
                    );

                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        if (!predictions.isEmpty()) {
                            PricePrediction prediction = predictions.get(0);

                            // Update queue with actual prediction
                            batchQueueList.getItems().set(currentIndex, prediction);

                            // Add to results table
                            batchResultsTable.getItems().add(prediction);

                            // Add to history
                            predictionHistory.add(prediction);
                        }

                        // Update progress
                        int done = completed.incrementAndGet();
                        double progress = (double) done / total;
                        batchProgressBar.setProgress(progress);
                        batchProgressLabel.setText(String.format("%d/%d (%.0f%%)",
                                done, total, progress * 100));

                        // If done, reset buttons
                        if (done == total) {
                            startBatchButton.setDisable(false);
                            pauseBatchButton.setDisable(true);
                            cancelBatchButton.setDisable(true);
                            batchProgressLabel.setText("✅ Batch processing completed");
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    final int errorIndex = currentIndex + 1; // Create final copy for lambda
                    javafx.application.Platform.runLater(() -> {
                        batchProgressLabel.setText("❌ Error at item " + errorIndex + ": " + e.getMessage());
                    });
                }
            } else {
                // Processing was cancelled
                break;
            }
        }
    }

    private void toggleBatchProcessing() {
        // For simplicity, we'll just simulate pausing
        if (pauseBatchButton.getText().contains("Pause")) {
            pauseBatchButton.setText("▶ Resume");
            batchProgressLabel.setText("⏸ Processing paused");
        } else {
            pauseBatchButton.setText("⏸ Pause");
            batchProgressLabel.setText("↻ Processing resumed");
        }
    }

    private void cancelBatchProcessing() {
        startBatchButton.setDisable(false);
        pauseBatchButton.setDisable(true);
        cancelBatchButton.setDisable(true);
        cancelBatchButton.setText("⏹ Cancel"); // Reset text
        batchProgressLabel.setText("⏹ Processing cancelled");
        batchProgressBar.setProgress(0);

        // Clear the queue to show processing stopped
        batchQueueList.getItems().clear();
    }

    private List<ExportData> generateBatchData(int size) {
        List<ExportData> batch = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            ProductType product = ProductType.values()[random.nextInt(ProductType.values().length)];
            LocalDate date = LocalDate.now().plusDays(random.nextInt(365));
            double price = switch (product) {
                case OLIVE_OIL -> 4000 + random.nextDouble() * 1000;
                case DATES -> 2500 + random.nextDouble() * 800;
                case CITRUS_FRUITS -> 1000 + random.nextDouble() * 500;
                case WHEAT -> 700 + random.nextDouble() * 200;
                case TOMATOES -> 800 + random.nextDouble() * 300;
                case PEPPERS -> 1300 + random.nextDouble() * 400;
            };

            double volume = 50 + random.nextDouble() * 150;
            String[] countries = {"France", "Germany", "Italy", "Spain", "UK", "USA"};
            String country = countries[random.nextInt(countries.length)];

            MarketIndicator[] indicators = MarketIndicator.values();
            MarketIndicator indicator = indicators[random.nextInt(indicators.length)];

            batch.add(new ExportData(
                    date, product, price, volume, country, indicator
            ));
        }

        return batch;
    }

    private void compareSelectedPredictions() {
        List<PricePrediction> selected = predictionHistoryList.getSelectionModel().getSelectedItems();

        if (selected.size() != 2) {
            showAlert("Comparison Error", "Please select exactly 2 predictions to compare");
            return;
        }

        PricePrediction p1 = selected.get(0);
        PricePrediction p2 = selected.get(1);

        ObservableList<ComparisonItem> comparisonData = FXCollections.observableArrayList();

        comparisonData.add(new ComparisonItem("Product",
                p1.productType().getFrenchName(),
                p2.productType().getFrenchName()));

        comparisonData.add(new ComparisonItem("Predicted Price",
                String.format("%.2f TND", p1.predictedPrice()),
                String.format("%.2f TND", p2.predictedPrice())));

        comparisonData.add(new ComparisonItem("Confidence",
                String.format("%.1f%%", p1.confidence() * 100),
                String.format("%.1f%%", p2.confidence() * 100)));

        comparisonData.add(new ComparisonItem("Prediction Date",
                p1.predictionDate().toString(),
                p2.predictionDate().toString()));

        comparisonData.add(new ComparisonItem("Risk Level",
                String.valueOf(p1.getRiskLevel()),
                String.valueOf(p2.getRiskLevel())));

        comparisonData.add(new ComparisonItem("Status",
                p1.status().toString(),
                p2.status().toString()));

        comparisonTable.setItems(comparisonData);

        // Update trend chart
        updateComparisonChart(p1, p2);
    }

    private void clearHistory() {
        predictionHistory.clear();
        comparisonTable.getItems().clear();
        trendChart.getData().clear();
    }

    private void runScenarioAnalysis() {
        try {
            // Get scenario parameters
            ProductType product = scenarioProductCombo.getValue();
            double priceChange = priceChangeSlider.getValue() / 100.0;
            double volumeChange = volumeChangeSlider.getValue() / 100.0;
            MarketIndicator market = scenarioMarketCombo.getValue();

            // Base data
            double basePrice = 3500.0; // Base price for the product
            double baseVolume = 100.0; // Base volume

            // Apply changes
            double newPrice = basePrice * (1 + priceChange);
            double newVolume = baseVolume * (1 + volumeChange);

            // Calculate impacts
            double baseRevenue = basePrice * baseVolume;
            double newRevenue = newPrice * newVolume;
            double revenueChange = ((newRevenue - baseRevenue) / baseRevenue) * 100;

            // Market impact factors
            double marketFactor = switch (market) {
                case STABLE -> 1.0;
                case VOLATILE -> 0.9;
                case RISING -> 1.2;
                case FALLING -> 0.8;
                case UNPREDICTABLE -> 0.7;
            };

            double adjustedRevenue = newRevenue * marketFactor;

            // Generate analysis report
            StringBuilder analysis = new StringBuilder();
            analysis.append("🧪 SCENARIO ANALYSIS RESULTS\n");
            analysis.append("══════════════════════════════\n\n");
            analysis.append(String.format("📦 Product: %s\n", product.getFrenchName()));
            analysis.append(String.format("📈 Price Change: %.1f%%\n", priceChange * 100));
            analysis.append(String.format("📊 Volume Change: %.1f%%\n", volumeChange * 100));
            analysis.append(String.format("🌍 Market Condition: %s\n", market.getDescription()));
            analysis.append("\n📊 FINANCIAL IMPACT:\n");
            analysis.append(String.format("• Base Revenue: %.2f TND\n", baseRevenue));
            analysis.append(String.format("• New Revenue: %.2f TND\n", newRevenue));
            analysis.append(String.format("• Market Adjusted Revenue: %.2f TND\n", adjustedRevenue));
            analysis.append(String.format("• Revenue Change: %.1f%%\n", revenueChange));
            analysis.append(String.format("• Market Impact Factor: %.2f\n", marketFactor));

            analysis.append("\n💡 STRATEGIC RECOMMENDATIONS:\n");
            if (revenueChange > 20) {
                analysis.append("✅ Excellent opportunity - Maximize production\n");
                analysis.append("✅ Target premium export markets\n");
                analysis.append("✅ Consider forward contracts\n");
            } else if (revenueChange > 5) {
                analysis.append("📈 Positive outlook - Increase production moderately\n");
                analysis.append("📈 Focus on quality improvement\n");
                analysis.append("📈 Diversify export destinations\n");
            } else if (revenueChange > -5) {
                analysis.append("⚖️ Neutral outlook - Maintain current strategy\n");
                analysis.append("⚖️ Monitor market closely\n");
                analysis.append("⚖️ Consider risk hedging\n");
            } else if (revenueChange > -20) {
                analysis.append("⚠️ Negative outlook - Reduce production\n");
                analysis.append("⚠️ Explore alternative products\n");
                analysis.append("⚠️ Focus on cost reduction\n");
            } else {
                analysis.append("❌ High risk - Consider production pause\n");
                analysis.append("❌ Explore new markets urgently\n");
                analysis.append("❌ Implement emergency cost measures\n");
            }

            scenarioResults.setText(analysis.toString());

            // Update scenario chart
            updateScenarioChart(priceChange * 100, volumeChange * 100, revenueChange);

        } catch (Exception e) {
            scenarioResults.setText("❌ Error in scenario analysis: " + e.getMessage());
        }
    }

    private void saveCurrentScenario() {
        String scenarioName = "Scenario_" + System.currentTimeMillis();

        Map<String, Object> scenario = new HashMap<>();
        scenario.put("product", scenarioProductCombo.getValue());
        scenario.put("priceChange", priceChangeSlider.getValue());
        scenario.put("volumeChange", volumeChangeSlider.getValue());
        scenario.put("marketCondition", scenarioMarketCombo.getValue());
        scenario.put("timestamp", LocalDate.now());

        savedScenarios.put(scenarioName, scenario);

        showAlert("Scenario Saved",
                String.format("Scenario '%s' saved successfully.\n%d scenarios total.",
                        scenarioName, savedScenarios.size()));
    }

    private void loadSavedScenario() {
        if (savedScenarios.isEmpty()) {
            showAlert("No Scenarios", "No saved scenarios found.");
            return;
        }

        // For simplicity, load the first scenario
        Map.Entry<String, Map<String, Object>> entry = savedScenarios.entrySet().iterator().next();
        Map<String, Object> scenario = entry.getValue();

        scenarioProductCombo.setValue((ProductType) scenario.get("product"));
        priceChangeSlider.setValue((Double) scenario.get("priceChange"));
        volumeChangeSlider.setValue((Double) scenario.get("volumeChange"));
        scenarioMarketCombo.setValue((MarketIndicator) scenario.get("marketCondition"));

        showAlert("Scenario Loaded",
                String.format("Loaded scenario: %s\nDated: %s",
                        entry.getKey(), scenario.get("timestamp")));
    }

    private void updateTrendChart(PricePrediction prediction) {
        // Add prediction to trend chart
        XYChart.Series<String, Number> series = trendChart.getData().isEmpty() ?
                new XYChart.Series<>() : trendChart.getData().get(0);

        String dateLabel = prediction.predictionDate().getMonth().toString().substring(0, 3);
        series.getData().add(new XYChart.Data<>(dateLabel, prediction.predictedPrice()));

        if (trendChart.getData().isEmpty()) {
            trendChart.getData().add(series);
        }
    }

    private void updateComparisonChart(PricePrediction p1, PricePrediction p2) {
        trendChart.getData().clear();

        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName(p1.productType().getFrenchName());
        series1.getData().add(new XYChart.Data<>("Price", p1.predictedPrice()));
        series1.getData().add(new XYChart.Data<>("Confidence", p1.confidence() * 100));

        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        series2.setName(p2.productType().getFrenchName());
        series2.getData().add(new XYChart.Data<>("Price", p2.predictedPrice()));
        series2.getData().add(new XYChart.Data<>("Confidence", p2.confidence() * 100));

        trendChart.getData().addAll(series1, series2);
    }

    private void updateScenarioChart(double priceChange, double volumeChange, double revenueChange) {
        scenarioChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Impact Analysis");
        series.getData().add(new XYChart.Data<>("Price Change", priceChange));
        series.getData().add(new XYChart.Data<>("Volume Change", volumeChange));
        series.getData().add(new XYChart.Data<>("Revenue Change", revenueChange));

        scenarioChart.getData().add(series);
    }

    private void loadSampleHistory() {
        try {
            // Load some sample predictions for demo
            List<PricePrediction> predictions = dataService.getPredictions();
            if (!predictions.isEmpty()) {
                // Take up to 5 predictions for the sample history
                int count = Math.min(predictions.size(), 5);
                predictionHistory.addAll(predictions.subList(0, count));
            } else {
                // Create sample predictions if none exist
                createSampleHistory();
            }
        } catch (Exception e) {
            System.err.println("Error loading sample history: " + e.getMessage());
            createSampleHistory();
        }
    }

    private void createSampleHistory() {
        // Create some sample predictions
        predictionHistory.add(new PricePrediction(
                LocalDate.now().plusDays(30),
                ProductType.OLIVE_OIL,
                4500.0,
                0.85,
                "DJL-Model",
                PredictionStatus.COMPLETED
        ));
        predictionHistory.add(new PricePrediction(
                LocalDate.now().plusDays(60),
                ProductType.DATES,
                2800.0,
                0.78,
                "DJL-Model",
                PredictionStatus.COMPLETED
        ));
        predictionHistory.add(new PricePrediction(
                LocalDate.now().plusDays(90),
                ProductType.CITRUS_FRUITS,
                1200.0,
                0.72,
                "DJL-Model",
                PredictionStatus.COMPLETED
        ));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class for comparison table
    private static class ComparisonItem {
        private final String metric;
        private final String value1;
        private final String value2;

        public ComparisonItem(String metric, String value1, String value2) {
            this.metric = metric;
            this.value1 = value1;
            this.value2 = value2;
        }

        public String getMetric() { return metric; }
        public String getValue1() { return value1; }
        public String getValue2() { return value2; }

        public javafx.beans.property.SimpleStringProperty metricProperty() {
            return new javafx.beans.property.SimpleStringProperty(metric);
        }

        public javafx.beans.property.SimpleStringProperty value1Property() {
            return new javafx.beans.property.SimpleStringProperty(value1);
        }

        public javafx.beans.property.SimpleStringProperty value2Property() {
            return new javafx.beans.property.SimpleStringProperty(value2);
        }
    }
}