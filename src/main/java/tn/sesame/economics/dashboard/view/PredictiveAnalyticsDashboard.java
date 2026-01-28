package tn.sesame.economics.dashboard.view;

import tn.sesame.economics.dashboard.service.DataService;
import tn.sesame.economics.model.*;
import tn.sesame.economics.service.EconomicIntelligenceService;
import tn.sesame.economics.ai.DJLPredictionService;
import tn.sesame.economics.ai.LLMReportService;
import tn.sesame.economics.util.DataLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
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
import java.util.stream.Collectors;

/**
 * FIXED: Predictive Analytics Dashboard with improved multi-selection UI
 * Changes:
 * 1. Added visual instructions panel
 * 2. Added selection counter label
 * 3. Added Select All / Deselect All buttons
 * 4. Added real-time selection validation
 * 5. Enhanced ListCell with visual feedback
 * 6. Improved error messages
 * 7. Smart enable/disable of compare button
 */
public class PredictiveAnalyticsDashboard extends VBox {

    private final DataService dataService;
    private EconomicIntelligenceService intelligenceService;
    private ExecutorService executorService;
    private List<ExportData> historicalData;

    private Queue<PricePrediction> predictionQueue = new LinkedList<>();
    private Deque<PricePrediction> predictionHistoryStack = new ArrayDeque<>();

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

    // History & Comparison Section - IMPROVED
    private ListView<PricePrediction> predictionHistoryList;
    private TableView<ComparisonItem> comparisonTable;
    private LineChart<String, Number> trendChart;
    private Button compareButton;
    private Button clearHistoryButton;
    private Button selectAllButton;      // NEW
    private Button deselectAllButton;    // NEW
    private Label selectionCountLabel;   // NEW
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

    private final Map<String, Map<String, Object>> savedScenarios;

    public PredictiveAnalyticsDashboard(DataService dataService) {
        this.dataService = dataService;
        this.executorService = Executors.newFixedThreadPool(2);
        this.predictionHistory = FXCollections.observableArrayList();
        this.savedScenarios = new HashMap<>();

        loadHistoricalData();
        initializeServices();
        initializeUI();
        loadSampleHistory();
        demonstrateQueueDequeUsage();
    }

    private void loadHistoricalData() {
        try {
            historicalData = DataLoader.loadCSVData("exports_historical.csv");
            if (historicalData.isEmpty()) {
                System.out.println("⚠️ No historical data found. Using fallback data...");
                historicalData = createFallbackData();
            } else {
                System.out.println("✅ Loaded " + historicalData.size() + " historical records");
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading historical data: " + e.getMessage());
            historicalData = createFallbackData();
        }
    }

    private List<ExportData> createFallbackData() {
        List<ExportData> fallbackData = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 50; i++) {
            ProductType product = ProductType.values()[random.nextInt(ProductType.values().length)];
            LocalDate date = LocalDate.now().minusDays(random.nextInt(365));
            double price = switch (product) {
                case OLIVE_OIL -> 3200 + random.nextDouble() * 1600;
                case DATES -> 2200 + random.nextDouble() * 1200;
                case CITRUS_FRUITS -> 1200 + random.nextDouble() * 1000;
                case WHEAT -> 700 + random.nextDouble() * 600;
                case TOMATOES -> 900 + random.nextDouble() * 800;
                case PEPPERS -> 1300 + random.nextDouble() * 1000;
            };
            double volume = 50 + random.nextDouble() * 200;
            String[] countries = {"France", "Germany", "Italy", "Spain", "UK", "USA"};
            String country = countries[random.nextInt(countries.length)];
            MarketIndicator indicator = MarketIndicator.values()[random.nextInt(MarketIndicator.values().length)];
            double volatility = 0.05 + random.nextDouble() * 0.25;
            double exchangeRate = 0.30 + random.nextDouble() * 0.03;

            fallbackData.add(new ExportData(
                    date, product, price, volume, country, indicator,
                    volatility, exchangeRate
            ));
        }

        return fallbackData;
    }

    private double[] getRealisticValuesForProduct(ProductType productType) {
        List<ExportData> productData = historicalData.stream()
                .filter(data -> data.productType() == productType)
                .collect(Collectors.toList());

        if (productData.isEmpty()) {
            double avgVolatility = historicalData.stream()
                    .mapToDouble(ExportData::priceVolatility)
                    .average()
                    .orElse(0.15);
            double avgExchangeRate = historicalData.stream()
                    .mapToDouble(ExportData::exchangeRateTNDUSD)
                    .average()
                    .orElse(0.315);

            return new double[]{avgVolatility, avgExchangeRate};
        }

        double avgVolatility = productData.stream()
                .mapToDouble(ExportData::priceVolatility)
                .average()
                .orElse(0.15);
        double avgExchangeRate = productData.stream()
                .mapToDouble(ExportData::exchangeRateTNDUSD)
                .average()
                .orElse(0.315);

        return new double[]{avgVolatility, avgExchangeRate};
    }

    private void initializeServices() {
        try {
            var predictionService = new DJLPredictionService();
            predictionService.loadModel();
            var reportService = new LLMReportService(true);
            this.intelligenceService = new EconomicIntelligenceService(predictionService, reportService);
        } catch (Exception e) {
            System.err.println("Failed to initialize AI services: " + e.getMessage());
        }
    }

    private void initializeUI() {
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().addAll(
                createRealTimeTab(),
                createBatchTab(),
                createHistoryTab(),      // IMPROVED VERSION
                createScenarioTab()
        );

        getChildren().add(tabPane);
    }

    private void demonstrateQueueDequeUsage() {
        System.out.println("=== QUEUE (FIFO) DEMONSTRATION ===");

        PricePrediction pred1 = createSamplePrediction(ProductType.OLIVE_OIL, 4500.0);
        PricePrediction pred2 = createSamplePrediction(ProductType.DATES, 2800.0);
        PricePrediction pred3 = createSamplePrediction(ProductType.CITRUS_FRUITS, 1200.0);

        predictionQueue.offer(pred1);
        predictionQueue.offer(pred2);
        predictionQueue.offer(pred3);

        System.out.println("Queue size after adding 3 items: " + predictionQueue.size());

        System.out.println("\nProcessing queue (FIFO order):");
        while (!predictionQueue.isEmpty()) {
            PricePrediction current = predictionQueue.poll();
            System.out.println("Processed: " + current.productType() + " - " + current.predictedPrice());
            predictionHistoryStack.push(current);
        }

        System.out.println("\n=== DEQUE (Double-ended) DEMONSTRATION ===");

        predictionHistoryStack.addFirst(createSamplePrediction(ProductType.WHEAT, 700.0));
        predictionHistoryStack.addLast(createSamplePrediction(ProductType.TOMATOES, 900.0));

        System.out.println("Deque size: " + predictionHistoryStack.size());
        System.out.println("First: " + predictionHistoryStack.peekFirst().productType());
        System.out.println("Last: " + predictionHistoryStack.peekLast().productType());

        PricePrediction removedFirst = predictionHistoryStack.removeFirst();
        PricePrediction removedLast = predictionHistoryStack.removeLast();
        System.out.println("Removed first: " + removedFirst.productType());
        System.out.println("Removed last: " + removedLast.productType());
        System.out.println("Deque size after removal: " + predictionHistoryStack.size());
    }

    private PricePrediction createSamplePrediction(ProductType product, double price) {
        return new PricePrediction(
                LocalDate.now().plusDays(30),
                product,
                price,
                0.85,
                "DJL-Model",
                PredictionStatus.COMPLETED
        );
    }

    private Tab createRealTimeTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #e8f5e9;");

        Label title = new Label("🔮 REAL-TIME PRICE PREDICTION");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));

        formGrid.add(new Label("Product Type:"), 0, 0);
        productCombo = new ComboBox<>();
        productCombo.getItems().addAll(ProductType.values());
        productCombo.setValue(ProductType.OLIVE_OIL);
        productCombo.setPrefWidth(200);
        formGrid.add(productCombo, 1, 0);

        formGrid.add(new Label("Destination Country:"), 0, 1);
        countryCombo = new ComboBox<>();
        countryCombo.getItems().addAll("France", "Germany", "Italy", "Spain", "UK", "USA", "Tunisia");
        countryCombo.setValue("France");
        formGrid.add(countryCombo, 1, 1);

        formGrid.add(new Label("Current Price (TND/ton):"), 0, 2);
        priceSpinner = new Spinner<>(0.0, 10000.0, 3500.0, 100.0);
        priceSpinner.setEditable(true);
        priceSpinner.setPrefWidth(150);
        formGrid.add(priceSpinner, 1, 2);

        formGrid.add(new Label("Volume (tons):"), 0, 3);
        volumeSpinner = new Spinner<>(1.0, 1000.0, 100.0, 10.0);
        volumeSpinner.setEditable(true);
        formGrid.add(volumeSpinner, 1, 3);

        formGrid.add(new Label("Market Condition:"), 0, 4);
        marketIndicatorCombo = new ComboBox<>();
        marketIndicatorCombo.getItems().addAll(MarketIndicator.values());
        marketIndicatorCombo.setValue(MarketIndicator.STABLE);
        formGrid.add(marketIndicatorCombo, 1, 4);

        predictButton = new Button("🤖 PREDICT FUTURE PRICE");
        predictButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 20px;");
        predictButton.setOnAction(e -> performRealTimePrediction());

        realTimeProgress = new ProgressIndicator();
        realTimeProgress.setVisible(false);
        realTimeProgress.setPrefSize(30, 30);

        HBox buttonBox = new HBox(10, predictButton, realTimeProgress);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        realTimeResults = new TextArea();
        realTimeResults.setPrefHeight(200);
        realTimeResults.setEditable(false);
        realTimeResults.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
        realTimeResults.setText("Enter parameters and click 'Predict' to see results here...");

        tabContent.getChildren().addAll(title, formGrid, buttonBox, realTimeResults);

        Tab tab = new Tab("🔮 Real-time", tabContent);
        tab.setTooltip(new Tooltip("Make individual price predictions"));
        return tab;
    }

    private void performRealTimePrediction() {
        try {
            realTimeProgress.setVisible(true);
            predictButton.setDisable(true);

            ProductType selectedProduct = productCombo.getValue();
            double[] realisticValues = getRealisticValuesForProduct(selectedProduct);
            double realisticVolatility = realisticValues[0];
            double realisticExchangeRate = realisticValues[1];

            ExportData input = new ExportData(
                    LocalDate.now().plusDays(30),
                    selectedProduct,
                    priceSpinner.getValue(),
                    volumeSpinner.getValue(),
                    countryCombo.getValue(),
                    marketIndicatorCombo.getValue(),
                    realisticVolatility,
                    realisticExchangeRate
            );

            executorService.submit(() -> {
                try {
                    List<ExportData> singleInput = List.of(input);
                    List<PricePrediction> predictions = intelligenceService.analyzeExports(singleInput);

                    javafx.application.Platform.runLater(() -> {
                        if (!predictions.isEmpty()) {
                            PricePrediction prediction = predictions.get(0);

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

                            realTimeResults.setText(result.toString());

                            predictionHistory.add(prediction);
                            predictionQueue.offer(prediction);
                            predictionHistoryStack.push(prediction);

                        } else {
                            realTimeResults.setText("❌ No prediction returned from model");
                        }

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

    private Tab createBatchTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #e3f2fd;");

        Label title = new Label("📦 BATCH PREDICTION PROCESSING");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1565c0;");

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

        batchProgressBar = new ProgressBar(0);
        batchProgressBar.setPrefWidth(400);
        batchProgressLabel = new Label("0% - Ready");

        VBox progressBox = new VBox(5, batchProgressBar, batchProgressLabel);

        Label queueLabel = new Label("Prediction Queue:");
        batchQueueList = new ListView<>();
        batchQueueList.setPrefHeight(150);

        Label resultsLabel = new Label("Batch Results:");
        batchResultsTable = new TableView<>();
        batchResultsTable.setPrefHeight(200);

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

        startBatchButton.setOnAction(e -> startBatchProcessing());
        pauseBatchButton.setOnAction(e -> toggleBatchProcessing());
        cancelBatchButton.setOnAction(e -> cancelBatchProcessing());

        tabContent.getChildren().addAll(title, controls, progressBox,
                queueLabel, batchQueueList,
                resultsLabel, batchResultsTable);

        Tab tab = new Tab("📦 Batch Processing", tabContent);
        tab.setTooltip(new Tooltip("Process multiple predictions at once"));
        return tab;
    }

    private void startBatchProcessing() {
        try {
            int batchSize = Integer.parseInt(batchSizeField.getText());
            if (batchSize <= 0) {
                batchProgressLabel.setText("Invalid batch size");
                return;
            }

            startBatchButton.setDisable(true);
            pauseBatchButton.setDisable(false);
            cancelBatchButton.setDisable(false);

            batchQueueList.getItems().clear();
            batchResultsTable.getItems().clear();

            List<ExportData> batchData = generateBatchData(batchSize);

            for (ExportData data : batchData) {
                PricePrediction pending = new PricePrediction(
                        LocalDate.now().plusDays(30),
                        data.productType(),
                        0.0,
                        0.0,
                        "Pending",
                        PredictionStatus.PENDING
                );
                batchQueueList.getItems().add(pending);
                predictionQueue.offer(pending);
            }

            executorService.submit(() -> processBatch(batchData));

        } catch (NumberFormatException e) {
            batchProgressLabel.setText("Invalid batch size format");
        }
    }

    private List<ExportData> generateBatchData(int size) {
        List<ExportData> batch = new ArrayList<>();
        Random random = new Random();

        if (!historicalData.isEmpty()) {
            for (int i = 0; i < Math.min(size, historicalData.size()); i++) {
                ExportData original = historicalData.get(i % historicalData.size());

                double priceVariation = 0.9 + (random.nextDouble() * 0.2);
                double volumeVariation = 0.8 + (random.nextDouble() * 0.4);

                ExportData modified = new ExportData(
                        original.date().plusDays(30),
                        original.productType(),
                        original.pricePerTon() * priceVariation,
                        original.volume() * volumeVariation,
                        original.destinationCountry(),
                        original.indicator(),
                        original.priceVolatility() * (0.8 + random.nextDouble() * 0.4),
                        original.exchangeRateTNDUSD() * (0.99 + random.nextDouble() * 0.02)
                );

                batch.add(modified);
            }

            if (size > historicalData.size()) {
                int remaining = size - historicalData.size();
                batch.addAll(generateRandomBatchData(remaining, random));
            }
        } else {
            batch = generateRandomBatchData(size, random);
        }

        return batch;
    }

    private List<ExportData> generateRandomBatchData(int size, Random random) {
        List<ExportData> batch = new ArrayList<>();

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

            MarketIndicator indicator = MarketIndicator.values()[random.nextInt(MarketIndicator.values().length)];

            double volatility = calculateRealisticVolatility(product, indicator, random);
            double exchangeRate = calculateRealisticExchangeRate(random);

            batch.add(new ExportData(
                    date, product, price, volume, country, indicator,
                    volatility, exchangeRate
            ));
        }

        return batch;
    }

    private double calculateRealisticVolatility(ProductType product, MarketIndicator indicator, Random random) {
        double baseVolatility = switch (product) {
            case OLIVE_OIL -> 0.15;
            case DATES -> 0.12;
            case CITRUS_FRUITS -> 0.20;
            case WHEAT -> 0.10;
            case TOMATOES -> 0.25;
            case PEPPERS -> 0.18;
        };

        double indicatorFactor = switch (indicator) {
            case VOLATILE -> 1.5;
            case UNPREDICTABLE -> 1.3;
            case RISING, FALLING -> 1.1;
            case STABLE -> 0.8;
        };

        double variation = 0.7 + (random.nextDouble() * 0.6);

        return Math.max(0.05, Math.min(0.5, baseVolatility * indicatorFactor * variation));
    }

    private double calculateRealisticExchangeRate(Random random) {
        return 0.315 + ((random.nextDouble() - 0.5) * 0.015);
    }

    private void processBatch(List<ExportData> batchData) {
        AtomicInteger completed = new AtomicInteger(0);
        int total = batchData.size();

        for (int i = 0; i < total; i++) {
            final int currentIndex = i;

            if (cancelButtonActive()) {
                break;
            }

            try {
                Thread.sleep(500);

                List<PricePrediction> predictions = intelligenceService.analyzeExports(
                        List.of(batchData.get(currentIndex))
                );

                javafx.application.Platform.runLater(() -> {
                    if (!predictions.isEmpty()) {
                        PricePrediction prediction = predictions.get(0);

                        if (currentIndex < batchQueueList.getItems().size()) {
                            batchQueueList.getItems().set(currentIndex, prediction);
                        }

                        batchResultsTable.getItems().add(prediction);
                        predictionHistory.add(prediction);
                        predictionHistoryStack.push(prediction);
                    }

                    int done = completed.incrementAndGet();
                    double progress = (double) done / total;
                    batchProgressBar.setProgress(progress);
                    batchProgressLabel.setText(String.format("%d/%d (%.0f%%)",
                            done, total, progress * 100));

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
                javafx.application.Platform.runLater(() -> {
                    batchProgressLabel.setText("❌ Error at item " + (currentIndex + 1) + ": " + e.getMessage());
                });
            }
        }
    }

    private boolean cancelButtonActive() {
        return cancelBatchButton.isDisabled();
    }

    private void toggleBatchProcessing() {
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
        batchProgressLabel.setText("⏹ Processing cancelled");
        batchProgressBar.setProgress(0);
        batchQueueList.getItems().clear();
        predictionQueue.clear();
    }

    /**
     * IMPROVED: History Tab with better multi-selection UI
     */
    private Tab createHistoryTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #fff3e0;");

        Label title = new Label("📊 PREDICTION HISTORY & COMPARISON");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ef6c00;");

        // NEW: Instructions panel
        VBox instructionsPanel = createInstructionsPanel();

        HBox mainContent = new HBox(20);

        // LEFT SIDE: History List with improved controls
        VBox historyBox = new VBox(10);
        historyBox.setPrefWidth(350);

        Label historyLabel = new Label("Prediction History:");
        historyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // NEW: Selection status label
        selectionCountLabel = new Label("Selected: 0 items (Select 2 to compare)");
        selectionCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        predictionHistoryList = new ListView<>(predictionHistory);
        predictionHistoryList.setPrefHeight(300);
        predictionHistoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // NEW: Customize cell factory for better visual feedback
        predictionHistoryList.setCellFactory(lv -> new ListCell<PricePrediction>() {
            @Override
            protected void updateItem(PricePrediction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(String.format("%s - %.2f TND (%.0f%% conf.)",
                            item.productType().getFrenchName(),
                            item.predictedPrice(),
                            item.confidence() * 100));

                    // Visual feedback for selected items
                    if (isSelected()) {
                        setStyle("-fx-background-color: #b3e5fc; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // NEW: Listen to selection changes and update the counter
        predictionHistoryList.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<PricePrediction>) change -> updateSelectionStatus());

        // NEW: Selection control buttons
        HBox selectionButtons = new HBox(10);
        selectAllButton = createControlButton("☑ Select All", "#4caf50");
        deselectAllButton = createControlButton("☐ Deselect All", "#9e9e9e");

        selectAllButton.setOnAction(e -> predictionHistoryList.getSelectionModel().selectAll());
        deselectAllButton.setOnAction(e -> predictionHistoryList.getSelectionModel().clearSelection());

        selectionButtons.getChildren().addAll(selectAllButton, deselectAllButton);

        // Action buttons
        HBox historyButtons = new HBox(10);
        compareButton = createControlButton("📊 Compare Selected", "#2196f3");
        clearHistoryButton = createControlButton("🗑 Clear History", "#f44336");

        compareButton.setOnAction(e -> compareSelectedPredictions());
        clearHistoryButton.setOnAction(e -> clearHistory());

        // NEW: Initially disable compare button
        compareButton.setDisable(true);

        historyButtons.getChildren().addAll(compareButton, clearHistoryButton);

        // NEW: Updated layout with all new components
        historyBox.getChildren().addAll(
                historyLabel,
                selectionCountLabel,
                selectionButtons,
                predictionHistoryList,
                historyButtons
        );

        // RIGHT SIDE: Comparison results
        VBox comparisonBox = new VBox(10);
        comparisonBox.setPrefWidth(450);

        Label comparisonLabel = new Label("Comparison Table:");
        comparisonLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        comparisonTable = new TableView<>();
        comparisonTable.setPrefHeight(150);

        TableColumn<ComparisonItem, String> itemCol = new TableColumn<>("Metric");
        itemCol.setCellValueFactory(cell -> cell.getValue().metricProperty());
        itemCol.setPrefWidth(150);

        TableColumn<ComparisonItem, String> value1Col = new TableColumn<>("Prediction 1");
        value1Col.setCellValueFactory(cell -> cell.getValue().value1Property());
        value1Col.setPrefWidth(140);

        TableColumn<ComparisonItem, String> value2Col = new TableColumn<>("Prediction 2");
        value2Col.setCellValueFactory(cell -> cell.getValue().value2Property());
        value2Col.setPrefWidth(140);

        comparisonTable.getColumns().addAll(itemCol, value1Col, value2Col);

        Label trendLabel = new Label("Price Trend Analysis:");
        trendChart = createTrendChart();
        trendChart.setPrefHeight(200);

        comparisonBox.getChildren().addAll(comparisonLabel, comparisonTable, trendLabel, trendChart);
        mainContent.getChildren().addAll(historyBox, comparisonBox);

        Tab tab = new Tab("📊 History & Comparison", tabContent);
        tab.setTooltip(new Tooltip("View and compare historical predictions"));
        tab.setContent(new VBox(10, title, instructionsPanel, mainContent));

        return tab;
    }

    /**
     * NEW: Create instructions panel to help users understand multi-selection
     */
    private VBox createInstructionsPanel() {
        VBox instructionsBox = new VBox(5);
        instructionsBox.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 10; -fx-border-color: #2196f3; -fx-border-radius: 5;");

        Label instructionsTitle = new Label("💡 How to Compare Predictions:");
        instructionsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label step1 = new Label("1️⃣ Hold CTRL (Windows/Linux) or CMD (Mac) while clicking to select multiple predictions");
        Label step2 = new Label("2️⃣ Or use 'Select All' button and then CTRL+Click to deselect unwanted items");
        Label step3 = new Label("3️⃣ Once you have exactly 2 predictions selected, click 'Compare Selected'");
        Label tip = new Label("💡 Tip: The selection counter shows how many items you've selected");

        step1.setStyle("-fx-font-size: 11px;");
        step2.setStyle("-fx-font-size: 11px;");
        step3.setStyle("-fx-font-size: 11px;");
        tip.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #1976d2;");

        instructionsBox.getChildren().addAll(instructionsTitle, step1, step2, step3, tip);

        return instructionsBox;
    }

    /**
     * NEW: Update selection status and enable/disable compare button
     */
    private void updateSelectionStatus() {
        int selectedCount = predictionHistoryList.getSelectionModel().getSelectedItems().size();

        selectionCountLabel.setText(String.format("Selected: %d items %s",
                selectedCount,
                selectedCount == 2 ? "✅ Ready to compare!" : "(Select 2 to compare)"));

        // Update label style based on selection
        if (selectedCount == 2) {
            selectionCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4caf50; -fx-font-weight: bold;");
            compareButton.setDisable(false);
        } else if (selectedCount > 2) {
            selectionCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff9800; -fx-font-weight: bold;");
            compareButton.setDisable(true);
        } else {
            selectionCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            compareButton.setDisable(true);
        }
    }

    private void compareSelectedPredictions() {
        List<PricePrediction> selected = predictionHistoryList.getSelectionModel().getSelectedItems();

        if (selected.size() != 2) {
            showAlert("Comparison Error",
                    "Please select exactly 2 predictions to compare.\n\n" +
                            "Current selection: " + selected.size() + " items\n\n" +
                            "How to select:\n" +
                            "• Hold CTRL (or CMD on Mac) while clicking items\n" +
                            "• Or use 'Select All' and deselect unwanted items");
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

        comparisonData.add(new ComparisonItem("Status",
                p1.status().toString(),
                p2.status().toString()));

        // NEW: Add price difference calculation
        double priceDiff = Math.abs(p1.predictedPrice() - p2.predictedPrice());
        double priceDiffPercent = (priceDiff / Math.min(p1.predictedPrice(), p2.predictedPrice())) * 100;
        comparisonData.add(new ComparisonItem("Price Difference",
                String.format("%.2f TND", priceDiff),
                String.format("%.1f%%", priceDiffPercent)));

        comparisonTable.setItems(comparisonData);
        updateComparisonChart(p1, p2);
    }

    private void clearHistory() {
        predictionHistory.clear();
        comparisonTable.getItems().clear();
        trendChart.getData().clear();
        predictionHistoryStack.clear();
        updateSelectionStatus();  // NEW: Reset the counter
    }

    private Tab createScenarioTab() {
        VBox tabContent = new VBox(15);
        tabContent.setPadding(new Insets(20));
        tabContent.setStyle("-fx-background-color: #f3e5f5;");

        Label title = new Label("🧪 WHAT-IF SCENARIO ANALYSIS");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7b1fa2;");

        HBox mainContent = new HBox(20);

        VBox controlsBox = new VBox(15);
        controlsBox.setPrefWidth(300);

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

        HBox scenarioButtons = new HBox(10);
        runScenarioButton = createControlButton("🧪 Run Scenario", "#9c27b0");
        saveScenarioButton = createControlButton("💾 Save Scenario", "#607d8b");
        loadScenarioButton = createControlButton("📂 Load Scenario", "#795548");

        runScenarioButton.setOnAction(e -> runScenarioAnalysis());
        saveScenarioButton.setOnAction(e -> saveCurrentScenario());
        loadScenarioButton.setOnAction(e -> loadSavedScenario());

        scenarioButtons.getChildren().addAll(runScenarioButton, saveScenarioButton, loadScenarioButton);
        controlsBox.getChildren().add(scenarioButtons);

        VBox resultsBox = new VBox(15);

        scenarioResults = new TextArea();
        scenarioResults.setPrefHeight(200);
        scenarioResults.setEditable(false);
        scenarioResults.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 12px;");

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

    private void runScenarioAnalysis() {
        try {
            ProductType product = scenarioProductCombo.getValue();
            double priceChange = priceChangeSlider.getValue() / 100.0;
            double volumeChange = volumeChangeSlider.getValue() / 100.0;
            MarketIndicator market = scenarioMarketCombo.getValue();

            double avgPrice = getAveragePriceForProduct(product);
            double avgVolume = getAverageVolumeForProduct(product);

            double basePrice = avgPrice;
            double baseVolume = avgVolume;

            double newPrice = basePrice * (1 + priceChange);
            double newVolume = baseVolume * (1 + volumeChange);

            double baseRevenue = basePrice * baseVolume;
            double newRevenue = newPrice * newVolume;
            double revenueChange = ((newRevenue - baseRevenue) / baseRevenue) * 100;

            double marketFactor = switch (market) {
                case STABLE -> 1.0;
                case VOLATILE -> 0.9;
                case RISING -> 1.2;
                case FALLING -> 0.8;
                case UNPREDICTABLE -> 0.7;
            };

            double adjustedRevenue = newRevenue * marketFactor;

            StringBuilder analysis = new StringBuilder();
            analysis.append("🧪 SCENARIO ANALYSIS RESULTS\n");
            analysis.append("══════════════════════════════\n\n");
            analysis.append(String.format("📦 Product: %s\n", product.getFrenchName()));
            analysis.append(String.format("📈 Price Change: %.1f%%\n", priceChange * 100));
            analysis.append(String.format("📊 Volume Change: %.1f%%\n", volumeChange * 100));
            analysis.append(String.format("🌍 Market Condition: %s\n", market));
            analysis.append("\n📊 FINANCIAL IMPACT:\n");
            analysis.append(String.format("• Base Revenue: %.2f TND\n", baseRevenue));
            analysis.append(String.format("• New Revenue: %.2f TND\n", newRevenue));
            analysis.append(String.format("• Market Adjusted Revenue: %.2f TND\n", adjustedRevenue));
            analysis.append(String.format("• Revenue Change: %.1f%%\n", revenueChange));
            analysis.append(String.format("• Market Impact Factor: %.2f\n", marketFactor));

            scenarioResults.setText(analysis.toString());
            updateScenarioChart(priceChange * 100, volumeChange * 100, revenueChange);

        } catch (Exception e) {
            scenarioResults.setText("❌ Error in scenario analysis: " + e.getMessage());
        }
    }

    private double getAveragePriceForProduct(ProductType productType) {
        return historicalData.stream()
                .filter(data -> data.productType() == productType)
                .mapToDouble(ExportData::pricePerTon)
                .average()
                .orElseGet(() -> {
                    switch (productType) {
                        case OLIVE_OIL: return 3500.0;
                        case DATES: return 2500.0;
                        case CITRUS_FRUITS: return 1500.0;
                        case WHEAT: return 900.0;
                        case TOMATOES: return 1200.0;
                        case PEPPERS: return 1600.0;
                        default: return 2000.0;
                    }
                });
    }

    private double getAverageVolumeForProduct(ProductType productType) {
        return historicalData.stream()
                .filter(data -> data.productType() == productType)
                .mapToDouble(ExportData::volume)
                .average()
                .orElse(100.0);
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
            List<PricePrediction> predictions = dataService.getPredictions();
            if (!predictions.isEmpty()) {
                int count = Math.min(predictions.size(), 5);
                predictionHistory.addAll(predictions.subList(0, count));
            } else {
                createSampleHistory();
            }
        } catch (Exception e) {
            System.err.println("Error loading sample history: " + e.getMessage());
            createSampleHistory();
        }
    }

    private void createSampleHistory() {
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